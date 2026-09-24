package com.kb.wms.inventory.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.InventoryStockUseCase;
import com.kb.wms.inventory.application.port.in.command.StockQuantityCommand;
import com.kb.wms.inventory.application.port.in.command.StockReceiveCommand;
import com.kb.wms.inventory.application.port.in.command.StockShipCommand;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.InventoryTransactionRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.application.port.out.SectionCapacityPort;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.entity.InventoryTransaction;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;
import com.kb.wms.inventory.exception.InventoryErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 입고·출고 도메인이 재고 수량을 바꾸는 유일한 경로.
 *
 * <p>호출자의 트랜잭션에 참여하며, 명령을 순서대로 처리하다 하나라도 실패하면 예외로 트랜잭션 전체가 롤백된다
 * (all-or-nothing). 잠금 순서는 구역 행(section_id 오름차순) → 재고 행(inventory_lot_id 오름차순)으로
 * 입고·출고 모두 고정해 교착을 막는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryStockService implements InventoryStockUseCase {

    private final InventoryLotRepository inventoryLotRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final LotRepository lotRepository;
    private final SectionCapacityPort sectionCapacityPort;

    /**
     * 구역 + 로트 재고 행에 보유 수량을 더한다(없으면 생성). 행마다 INBOUND 이력을 남긴다.
     * 관련 구역 행을 먼저 잠가, 같은 (구역, 로트)의 첫 입고가 동시에 와도 재고 행을 하나만 만든다
     * (구역 잠금을 기다린 두 번째 요청은 첫 요청이 만든 행을 찾아 수량을 더한다).
     */
    @Override
    public List<InventoryLot> receive(List<StockReceiveCommand> commands) {
        List<StockReceiveCommand> ordered = commands.stream()
                .sorted(Comparator.comparing(StockReceiveCommand::sectionId)
                        .thenComparing(StockReceiveCommand::lotId))
                .toList();
        sectionCapacityPort.lock(ordered.stream().map(StockReceiveCommand::sectionId).toList());

        List<InventoryLot> results = new ArrayList<>();
        for (StockReceiveCommand command : ordered) {
            requirePositive(command.quantity(), "입고 수량");
            Lot lot = lotRepository.findById(command.lotId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, LotService.LOT_NOT_FOUND_MESSAGE));
            if (!lot.isAvailable()) {
                throw new BusinessException(InventoryErrorCode.LOT_NOT_AVAILABLE);
            }

            InventoryLot inventoryLot = inventoryLotRepository
                    .findBySectionIdAndLotIdForUpdate(command.sectionId(), command.lotId())
                    .orElseGet(() -> InventoryLot.open(command.sectionId(), command.lotId(), command.qualityStatus()));
            if (inventoryLot.getQualityStatus() != command.qualityStatus()) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "같은 구역·로트의 기존 재고와 품질 상태가 다릅니다.");
            }

            sectionCapacityPort.occupy(command.sectionId(), command.quantity());
            long before = inventoryLot.getOnHandQuantity();
            inventoryLot.increase(command.quantity());
            InventoryLot saved = inventoryLotRepository.save(inventoryLot);

            inventoryTransactionRepository.save(InventoryTransaction.record(
                    saved.getInventoryLotId(), TransactionType.INBOUND, before, saved.getOnHandQuantity(),
                    ReferenceType.INBOUND, command.inboundId(), null, command.userId()));
            results.add(saved);
        }
        return results;
    }

    /**
     * 가용 수량(품질 AVAILABLE, on_hand - allocated)에서 예약한다. 보유 수량이 그대로라 이력은 남기지 않는다.
     * 로트가 AVAILABLE이 아니면(만료·격리 등) LOT_NOT_AVAILABLE로 거절한다.
     */
    @Override
    public List<InventoryLot> allocate(List<StockQuantityCommand> commands) {
        Map<Long, InventoryLot> locked = lockAll(commands.stream().map(StockQuantityCommand::inventoryLotId).toList());
        for (StockQuantityCommand command : commands) {
            requirePositive(command.quantity(), "할당 수량");
            InventoryLot inventoryLot = locked.get(command.inventoryLotId());
            Lot lot = lotRepository.findById(inventoryLot.getLotId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, LotService.LOT_NOT_FOUND_MESSAGE));
            if (!lot.isAvailable()) {
                throw new BusinessException(InventoryErrorCode.LOT_NOT_AVAILABLE);
            }
            if (!inventoryLot.canAllocate(command.quantity())) {
                throw new BusinessException(InventoryErrorCode.INSUFFICIENT_STOCK,
                        "가용 재고가 부족합니다. (재고 " + inventoryLot.getInventoryLotId()
                                + ": 요청 " + command.quantity() + ", 가용 " + availableOf(inventoryLot) + ")");
            }
            inventoryLot.allocate(command.quantity());
        }
        return saveAll(locked);
    }

    @Override
    public List<InventoryLot> release(List<StockQuantityCommand> commands) {
        Map<Long, InventoryLot> locked = lockAll(commands.stream().map(StockQuantityCommand::inventoryLotId).toList());
        for (StockQuantityCommand command : commands) {
            requirePositive(command.quantity(), "해제 수량");
            InventoryLot inventoryLot = locked.get(command.inventoryLotId());
            if (!inventoryLot.canRelease(command.quantity())) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "할당 수량보다 많이 해제할 수 없습니다. (재고 " + inventoryLot.getInventoryLotId()
                                + ": 요청 " + command.quantity() + ", 할당 " + inventoryLot.getAllocatedQuantity() + ")");
            }
            inventoryLot.release(command.quantity());
        }
        return saveAll(locked);
    }

    /**
     * 피킹 완료: 할당 전체를 풀고 피킹 수량만큼 보유 수량을 차감한다.
     * 부족분(allocated - picked)의 예약은 풀려 다시 가용 재고가 된다. 피킹 수량이 있으면 OUTBOUND 이력을 남긴다.
     */
    @Override
    public List<InventoryLot> ship(List<StockShipCommand> commands) {
        // 입고와 같은 잠금 순서(구역 → 재고 행)를 지키기 위해, 재고 행의 구역을 먼저 읽어 구역부터 잠근다.
        sectionCapacityPort.lock(commands.stream()
                .map(StockShipCommand::inventoryLotId).distinct()
                .map(id -> inventoryLotRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                                InventoryQueryService.INVENTORY_NOT_FOUND_MESSAGE + " (재고 " + id + ")")))
                .map(InventoryLot::getSectionId)
                .toList());
        Map<Long, InventoryLot> locked = lockAll(commands.stream().map(StockShipCommand::inventoryLotId).toList());
        for (StockShipCommand command : commands) {
            requirePositive(command.allocatedQuantity(), "할당 수량");
            if (command.pickedQuantity() < 0 || command.pickedQuantity() > command.allocatedQuantity()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "피킹 수량은 0 이상, 할당 수량(" + command.allocatedQuantity() + ") 이하여야 합니다.");
            }
            InventoryLot inventoryLot = locked.get(command.inventoryLotId());
            if (!inventoryLot.canRelease(command.allocatedQuantity())) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "재고의 할당 수량이 출고 할당 수량보다 적습니다. (재고 " + inventoryLot.getInventoryLotId() + ")");
            }

            long shortage = command.allocatedQuantity() - command.pickedQuantity();
            if (shortage > 0) {
                inventoryLot.release(shortage);
            }
            if (command.pickedQuantity() > 0) {
                long before = inventoryLot.getOnHandQuantity();
                inventoryLot.ship(command.pickedQuantity());
                sectionCapacityPort.vacate(inventoryLot.getSectionId(), command.pickedQuantity());
                inventoryTransactionRepository.save(InventoryTransaction.record(
                        inventoryLot.getInventoryLotId(), TransactionType.OUTBOUND, before,
                        inventoryLot.getOnHandQuantity(), ReferenceType.OUTBOUND, command.outboundId(), null,
                        command.userId()));
            }
        }
        return saveAll(locked);
    }

    /** 중복을 제거한 ID를 오름차순으로 잠근다. 하나라도 없으면 NOT_FOUND. */
    private Map<Long, InventoryLot> lockAll(List<Long> inventoryLotIds) {
        List<Long> ids = inventoryLotIds.stream().distinct().sorted().toList();
        Map<Long, InventoryLot> locked = inventoryLotRepository.findAllByIdsForUpdate(ids).stream()
                .collect(Collectors.toMap(InventoryLot::getInventoryLotId, Function.identity(),
                        (a, b) -> a, java.util.LinkedHashMap::new));
        ids.stream()
                .filter(id -> !locked.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new BusinessException(ErrorCode.NOT_FOUND,
                            InventoryQueryService.INVENTORY_NOT_FOUND_MESSAGE + " (재고 " + id + ")");
                });
        return locked;
    }

    private List<InventoryLot> saveAll(Map<Long, InventoryLot> locked) {
        return locked.values().stream().map(inventoryLotRepository::save).toList();
    }

    private static long availableOf(InventoryLot inventoryLot) {
        return inventoryLot.isDefective() ? 0 : inventoryLot.availableQuantity();
    }

    private static void requirePositive(long quantity, String name) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, name + "은(는) 0보다 커야 합니다.");
        }
    }
}

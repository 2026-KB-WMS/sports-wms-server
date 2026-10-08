package com.kb.wms.outbound.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.outbound.application.port.in.OutboundFulfillmentUseCase;
import com.kb.wms.outbound.application.port.in.command.OutboundPickingCompleteCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundPickingCompleteCommand.PickedLine;
import com.kb.wms.outbound.application.port.in.result.OutboundDeliverResult;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingCompleteResult;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingCompleteResult.InventoryState;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingCompleteResult.PickedItem;
import com.kb.wms.outbound.application.port.in.result.OutboundShipResult;
import com.kb.wms.outbound.application.port.in.result.OutboundView;
import com.kb.wms.outbound.application.port.out.OutboundQueryRepository;
import com.kb.wms.outbound.application.port.out.OutboundRepository;
import com.kb.wms.outbound.application.port.out.OutboundStockPort;
import com.kb.wms.outbound.application.port.out.OutboundStockPort.StockPick;
import com.kb.wms.outbound.application.port.out.OutboundStockPort.StockState;
import com.kb.wms.outbound.application.port.out.SkuSupplyPricePort;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.entity.OutboundLine;
import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.outbound.exception.OutboundErrorCode;
import com.kb.wms.storeorder.application.port.in.StoreOrderFulfillmentUseCase;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderLinePickedCommand;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;

import lombok.RequiredArgsConstructor;

/**
 * 피킹 완료·배송 시작·배송 완료(서비스 C). 재고 차감(피킹 완료)과 발주 수량·상태 전환(피킹 완료·배송 완료)이 들어 있다.
 * 잠금 순서는 발주 헤더 → 출고 헤더 → 할당 → 발주 항목이고, 재고 행은 재고 서비스가 잠근다(서비스 B와 같다).
 * 전역 락 순서는 성능 개선 단계에서 다룬다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboundFulfillmentService implements OutboundFulfillmentUseCase {

    private static final List<OutboundStatus> IN_PROGRESS = List.of(
            OutboundStatus.READY, OutboundStatus.PICKING, OutboundStatus.PICKED, OutboundStatus.SHIPPED);

    private final OutboundRepository outboundRepository;
    private final StockAllocationRepository stockAllocationRepository;
    private final OutboundQueryRepository outboundQueryRepository;
    private final OutboundStockPort outboundStockPort;
    private final SkuSupplyPricePort skuSupplyPricePort;
    private final StoreOrderFulfillmentUseCase storeOrderFulfillmentUseCase;
    private final StatusHistoryUseCase statusHistoryUseCase;

    @Override
    @Transactional
    public OutboundPickingCompleteResult completePicking(OutboundPickingCompleteCommand command,
                                                         AuthenticatedUser actor) {
        requireUser(command.userId());
        Outbound outbound = lockOutbound(command.outboundId(), actor);
        if (outbound.getStatus() != OutboundStatus.PICKING) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "PICKING 상태의 출고만 피킹을 완료할 수 있습니다. 현재 상태: " + outbound.getStatus());
        }

        List<OutboundLine> lines = outboundRepository.findLinesByOutboundId(outbound.getOutboundId());
        Map<Long, Long> pickedByLine = validatePickedLines(command.lines(), lines);

        List<Long> allocationIds = lines.stream().map(OutboundLine::getAllocationId).sorted().toList();
        Map<Long, StockAllocation> allocations = stockAllocationRepository.findAllByIdForUpdate(allocationIds)
                .stream().collect(Collectors.toMap(StockAllocation::getAllocationId, Function.identity()));
        if (allocations.size() != allocationIds.size()
                || allocations.values().stream().anyMatch(a -> a.getStatus() != AllocationStatus.ALLOCATED)) {
            throw new BusinessException(OutboundErrorCode.ALLOCATION_NOT_ACTIVE);
        }
        validateNotExceeding(lines, allocations, pickedByLine);
        if (pickedByLine.values().stream().mapToLong(Long::longValue).sum() == 0) {
            throw new BusinessException(OutboundErrorCode.NOTHING_PICKED);
        }

        Map<Long, StoreOrderLine> orderLines = storeOrderFulfillmentUseCase
                .getLinesForUpdate(outbound.getStoreOrderId()).stream()
                .collect(Collectors.toMap(StoreOrderLine::getStoreOrderLineId, Function.identity()));
        Map<Long, BigDecimal> prices = resolvePrices(lines, allocations, orderLines);

        List<StockPick> picks = lines.stream().map(line -> {
            StockAllocation allocation = allocations.get(line.getAllocationId());
            return new StockPick(allocation.getInventoryLotId(), allocation.getAllocatedQuantity(),
                    pickedByLine.get(line.getOutboundLineId()));
        }).toList();
        Map<Long, StockState> states = outboundStockPort.ship(outbound.getOutboundId(), command.userId(), picks)
                .stream().collect(Collectors.toMap(StockState::inventoryLotId, Function.identity(), (a, b) -> b));

        List<StoreOrderLinePickedCommand> orderLineCommands = lines.stream().map(line -> {
            StockAllocation allocation = allocations.get(line.getAllocationId());
            return new StoreOrderLinePickedCommand(allocation.getStoreOrderLineId(),
                    allocation.getAllocatedQuantity(), pickedByLine.get(line.getOutboundLineId()));
        }).toList();

        for (OutboundLine line : lines) {
            StockAllocation allocation = allocations.get(line.getAllocationId());
            long picked = pickedByLine.get(line.getOutboundLineId());
            allocation.pick(picked);
            line.confirmPicking(picked, allocation.getAllocatedQuantity(), prices.get(line.getOutboundLineId()));
        }
        stockAllocationRepository.saveAll(List.copyOf(allocations.values()));
        List<OutboundLine> savedLines = outboundRepository.saveLines(lines);
        storeOrderFulfillmentUseCase.applyPicked(orderLineCommands);

        outbound.completePicking();
        outboundRepository.save(outbound);
        for (OutboundLine line : lines) {
            statusHistoryUseCase.record(StatusHistoryEntityType.STOCK_ALLOCATION, line.getAllocationId(),
                    AllocationStatus.ALLOCATED.name(), AllocationStatus.PICKED.name(), null, command.userId());
        }
        statusHistoryUseCase.record(StatusHistoryEntityType.OUTBOUND, outbound.getOutboundId(),
                OutboundStatus.PICKING.name(), OutboundStatus.PICKED.name(), null, command.userId());

        List<PickedItem> items = savedLines.stream().map(line -> {
            StockAllocation allocation = allocations.get(line.getAllocationId());
            StockState state = states.get(allocation.getInventoryLotId());
            return new PickedItem(line.getOutboundLineId(), allocation.getAllocationId(),
                    allocation.getAllocatedQuantity(), pickedByLine.get(line.getOutboundLineId()),
                    line.getConfirmedUnitSupplyPrice(),
                    new InventoryState(state.inventoryLotId(), state.onHandQuantity(), state.allocatedQuantity()));
        }).toList();
        OutboundView view = findViewOrThrow(outbound.getOutboundId());
        return new OutboundPickingCompleteResult(view.outboundId(), view.outboundNo(), view.status(),
                items.stream().anyMatch(i -> i.shortageQuantity() > 0), items, view.updatedAt());
    }

    @Override
    @Transactional
    public OutboundShipResult ship(Long outboundId, AuthenticatedUser actor) {
        Long userId = actor.userId();
        Outbound outbound = lockOutbound(outboundId, actor);
        if (outbound.getStatus() != OutboundStatus.PICKED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "PICKED 상태의 출고만 배송을 시작할 수 있습니다. 현재 상태: " + outbound.getStatus());
        }
        outbound.ship(LocalDateTime.now(), userId);
        outboundRepository.save(outbound);
        statusHistoryUseCase.record(StatusHistoryEntityType.OUTBOUND, outboundId,
                OutboundStatus.PICKED.name(), OutboundStatus.SHIPPED.name(), null, userId);

        OutboundView view = findViewOrThrow(outboundId);
        return new OutboundShipResult(view.outboundId(), view.outboundNo(), view.status(), view.shippedAt(),
                view.shippedBy(), view.updatedAt());
    }

    @Override
    @Transactional
    public OutboundDeliverResult deliver(Long outboundId, AuthenticatedUser actor) {
        Long userId = actor.userId();
        Outbound outbound = lockOutbound(outboundId, actor);
        if (outbound.getStatus() != OutboundStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "SHIPPED 상태의 출고만 배송을 완료할 수 있습니다. 현재 상태: " + outbound.getStatus());
        }
        outbound.deliver(LocalDateTime.now());
        outboundRepository.save(outbound);
        statusHistoryUseCase.record(StatusHistoryEntityType.OUTBOUND, outboundId,
                OutboundStatus.SHIPPED.name(), OutboundStatus.DELIVERED.name(), null, userId);

        List<Long> allocationIds = outboundRepository.findLinesByOutboundId(outboundId).stream()
                .map(OutboundLine::getAllocationId).sorted().toList();
        Set<Long> orderLineIds = stockAllocationRepository.findAllByIdForUpdate(allocationIds).stream()
                .map(StockAllocation::getStoreOrderLineId).collect(Collectors.toSet());
        storeOrderFulfillmentUseCase.refreshLineStatuses(orderLineIds);

        Long storeOrderId = outbound.getStoreOrderId();
        StoreOrder order;
        if (outboundRepository.existsByStoreOrderIdAndStatusIn(storeOrderId, IN_PROGRESS)) {
            order = storeOrderFulfillmentUseCase.getOrder(storeOrderId);
        } else {
            order = storeOrderFulfillmentUseCase.completeIfFulfilled(storeOrderId, userId);
        }

        OutboundView view = findViewOrThrow(outboundId);
        return new OutboundDeliverResult(view.outboundId(), view.outboundNo(), view.status(), view.deliveredAt(),
                storeOrderId, order.getStatus(), view.updatedAt());
    }

    /** 항목 수·ID 일치, 중복·null·음수 수량을 검증하고 출고 항목 ID → 피킹 수량 맵을 돌려준다. */
    private Map<Long, Long> validatePickedLines(List<PickedLine> requested, List<OutboundLine> lines) {
        if (requested == null || requested.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "피킹 항목(lines)은 필수입니다.");
        }
        if (requested.size() != lines.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "출고의 모든 항목을 입력해야 합니다. 출고 항목 " + lines.size() + "개, 입력 " + requested.size() + "개");
        }
        Set<Long> lineIds = lines.stream().map(OutboundLine::getOutboundLineId).collect(Collectors.toSet());
        Set<Long> seen = new HashSet<>();
        Map<Long, Long> picked = new java.util.HashMap<>();
        for (PickedLine line : requested) {
            if (line == null || line.outboundLineId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "출고 항목 ID(outboundLineId)는 필수입니다.");
            }
            if (!lineIds.contains(line.outboundLineId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "이 출고의 항목이 아닙니다. outboundLineId=" + line.outboundLineId());
            }
            if (!seen.add(line.outboundLineId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "출고 항목이 중복되었습니다. outboundLineId=" + line.outboundLineId());
            }
            if (line.pickedQuantity() == null || line.pickedQuantity() < 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "피킹 수량은 0 이상이어야 합니다. outboundLineId=" + line.outboundLineId());
            }
            picked.put(line.outboundLineId(), line.pickedQuantity());
        }
        return picked;
    }

    private void validateNotExceeding(List<OutboundLine> lines, Map<Long, StockAllocation> allocations,
            Map<Long, Long> pickedByLine) {
        for (OutboundLine line : lines) {
            long allocated = allocations.get(line.getAllocationId()).getAllocatedQuantity();
            long picked = pickedByLine.get(line.getOutboundLineId());
            if (picked > allocated) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "피킹 수량이 할당 수량을 넘을 수 없습니다. outboundLineId=" + line.getOutboundLineId()
                                + " 할당 " + allocated + ", 피킹 " + picked);
            }
        }
    }

    /** 출고 항목별 확정 공급 단가. 발주 항목 스냅샷 → SKU 현재 단가 순이며, 둘 다 없으면 SUPPLY_PRICE_MISSING. */
    private Map<Long, BigDecimal> resolvePrices(List<OutboundLine> lines, Map<Long, StockAllocation> allocations,
            Map<Long, StoreOrderLine> orderLines) {
        Map<Long, BigDecimal> prices = new java.util.HashMap<>();
        for (OutboundLine line : lines) {
            StoreOrderLine orderLine = orderLines.get(allocations.get(line.getAllocationId()).getStoreOrderLineId());
            if (orderLine == null) {
                throw new BusinessException(OutboundErrorCode.ALLOCATION_NOT_ACTIVE);
            }
            BigDecimal price = orderLine.getRequestedUnitSupplyPrice() != null
                    ? orderLine.getRequestedUnitSupplyPrice()
                    : skuSupplyPricePort.findCurrentSupplyPrice(orderLine.getSkuId()).orElse(null);
            if (price == null) {
                throw new BusinessException(OutboundErrorCode.SUPPLY_PRICE_MISSING);
            }
            prices.put(line.getOutboundLineId(), price);
        }
        return prices;
    }

    /** 발주 → 출고 순으로 잠근다(서비스 B와 같은 순서). */
    private Outbound lockOutbound(Long outboundId, AuthenticatedUser actor) {
        if (outboundId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "출고를 선택해주세요.");
        }
        OutboundView view = findViewOrThrow(outboundId);
        StoreOrder order = storeOrderFulfillmentUseCase.getOrderForUpdate(view.storeOrderId());
        actor.requireWarehouseAccess(order.getWarehouseId());
        return outboundRepository.findByIdForUpdate(outboundId)
                .orElseThrow(() -> new BusinessException(OutboundErrorCode.OUTBOUND_NOT_FOUND));
    }

    private OutboundView findViewOrThrow(Long outboundId) {
        return outboundQueryRepository.findOutboundView(outboundId)
                .orElseThrow(() -> new BusinessException(OutboundErrorCode.OUTBOUND_NOT_FOUND));
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청 사용자는 필수입니다.");
        }
    }
}

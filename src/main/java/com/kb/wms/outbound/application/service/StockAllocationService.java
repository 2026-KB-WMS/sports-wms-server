package com.kb.wms.outbound.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.outbound.application.port.in.StockAllocationUseCase;
import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;
import com.kb.wms.outbound.application.port.in.command.StockAllocationReleaseCommand;
import com.kb.wms.outbound.application.port.in.query.StockAllocationSearchCondition;
import com.kb.wms.outbound.application.port.in.result.FefoStockCandidate;
import com.kb.wms.outbound.application.port.in.result.StockAllocateResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationDetail;
import com.kb.wms.outbound.application.port.in.result.StockAllocationReleaseResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationSummary;
import com.kb.wms.outbound.application.port.in.result.StockAllocationView;
import com.kb.wms.outbound.application.port.out.OutboundQueryRepository;
import com.kb.wms.outbound.application.port.out.OutboundStockPort;
import com.kb.wms.outbound.application.port.out.OutboundStockPort.StockQuantity;
import com.kb.wms.outbound.application.port.out.OutboundStockPort.StockState;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.exception.OutboundErrorCode;
import com.kb.wms.storeorder.application.port.in.StoreOrderFulfillmentUseCase;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderLineQuantityCommand;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

import lombok.RequiredArgsConstructor;

/**
 * 재고 할당·해제 서비스(서비스 A). 상태 확인과 변경은 발주 헤더를 잠근 상태에서 한 트랜잭션으로 처리하고,
 * 재고 행 잠금과 수량 변경은 재고 서비스에 맡긴다. 전역 락 순서와 구역 잠금은 성능 개선 단계에서 다룬다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockAllocationService implements StockAllocationUseCase {

    static final int REASON_MAX_LENGTH = 500;

    private final StockAllocationRepository stockAllocationRepository;
    private final OutboundQueryRepository outboundQueryRepository;
    private final OutboundStockPort outboundStockPort;
    private final StoreOrderFulfillmentUseCase storeOrderFulfillmentUseCase;
    private final StatusHistoryUseCase statusHistoryUseCase;

    @Override
    @Transactional
    public StockAllocateResult allocate(StockAllocateCommand command) {
        requireUser(command.userId());
        if (command.storeOrderId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "할당할 발주를 선택해주세요.");
        }

        StoreOrder order = storeOrderFulfillmentUseCase.getOrderForUpdate(command.storeOrderId());
        if (order.getStatus() != StoreOrderStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "배정된 발주만 재고를 할당할 수 있습니다. 현재 상태: " + order.getStatus());
        }

        List<StoreOrderLine> targets = storeOrderFulfillmentUseCase.getLinesForUpdate(order.getStoreOrderId()).stream()
                .filter(line -> line.unallocatedQuantity() > 0)
                .toList();
        if (targets.isEmpty()) {
            throw new BusinessException(OutboundErrorCode.ALREADY_ALLOCATED);
        }

        List<Placement> placements = placeByFefo(order.getWarehouseId(), targets);

        LocalDateTime now = LocalDateTime.now();
        List<StockAllocation> saved = stockAllocationRepository.saveAll(placements.stream()
                .map(p -> StockAllocation.allocate(p.storeOrderLineId(), p.inventoryLotId(), p.quantity(),
                        command.userId(), now))
                .toList());

        outboundStockPort.allocate(sumBy(placements, Placement::inventoryLotId).entrySet().stream()
                .map(e -> new StockQuantity(e.getKey(), e.getValue()))
                .toList());
        storeOrderFulfillmentUseCase.increaseAllocated(sumBy(placements, Placement::storeOrderLineId).entrySet().stream()
                .map(e -> new StoreOrderLineQuantityCommand(e.getKey(), e.getValue()))
                .toList());

        for (StockAllocation allocation : saved) {
            statusHistoryUseCase.record(StatusHistoryEntityType.STOCK_ALLOCATION, allocation.getAllocationId(),
                    null, AllocationStatus.ALLOCATED.name(), null, command.userId());
        }

        List<StockAllocationSummary> items = outboundQueryRepository.findAllocationSummaries(
                saved.stream().map(StockAllocation::getAllocationId).toList());
        return new StockAllocateResult(order.getStoreOrderId(), order.getOrderNo(), items);
    }

    @Override
    @Transactional
    public StockAllocationReleaseResult release(StockAllocationReleaseCommand command) {
        requireUser(command.userId());
        String reason = normalizeReason(command.reason());
        if (reason == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "할당 해제 사유를 입력해주세요.");
        }
        if (reason.length() > REASON_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "해제 사유는 " + REASON_MAX_LENGTH + "자 이하여야 합니다.");
        }

        // 발주 → 할당 순으로 잠그기 위해, 잠그지 않는 조회로 발주를 먼저 찾는다.
        StockAllocationView view = outboundQueryRepository.findAllocationView(command.allocationId())
                .orElseThrow(() -> new BusinessException(OutboundErrorCode.ALLOCATION_NOT_FOUND));
        storeOrderFulfillmentUseCase.getOrderForUpdate(view.storeOrderId());
        StockAllocation allocation = stockAllocationRepository.findByIdForUpdate(command.allocationId())
                .orElseThrow(() -> new BusinessException(OutboundErrorCode.ALLOCATION_NOT_FOUND));

        if (allocation.getStatus() != AllocationStatus.ALLOCATED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "할당 상태의 재고 할당만 해제할 수 있습니다. 현재 상태: " + allocation.getStatus());
        }
        if (stockAllocationRepository.isLinkedToActiveOutbound(allocation.getAllocationId())) {
            throw new BusinessException(OutboundErrorCode.ALLOCATION_IN_OUTBOUND);
        }

        allocation.release(LocalDateTime.now());
        StockAllocation saved = stockAllocationRepository.save(allocation);

        List<StockState> states = outboundStockPort.release(
                List.of(new StockQuantity(saved.getInventoryLotId(), saved.getAllocatedQuantity())));
        storeOrderFulfillmentUseCase.decreaseAllocated(
                List.of(new StoreOrderLineQuantityCommand(saved.getStoreOrderLineId(), saved.getAllocatedQuantity())));
        statusHistoryUseCase.record(StatusHistoryEntityType.STOCK_ALLOCATION, saved.getAllocationId(),
                AllocationStatus.ALLOCATED.name(), AllocationStatus.RELEASED.name(), reason, command.userId());

        StockState state = states.get(0);
        return new StockAllocationReleaseResult(saved.getAllocationId(), saved.getStatus(),
                saved.getAllocatedQuantity(), saved.getReleasedAt(), state.inventoryLotId(),
                state.onHandQuantity(), state.allocatedQuantity(), state.availableQuantity());
    }

    @Override
    public List<StockAllocationSummary> searchAllocations(StockAllocationSearchCondition condition) {
        return outboundQueryRepository.searchAllocations(condition);
    }

    @Override
    public StockAllocationDetail getAllocation(Long allocationId) {
        StockAllocationView view = outboundQueryRepository.findAllocationView(allocationId)
                .orElseThrow(() -> new BusinessException(OutboundErrorCode.ALLOCATION_NOT_FOUND));
        Long outboundId = outboundQueryRepository.findActiveOutboundIdByAllocationId(allocationId).orElse(null);
        return new StockAllocationDetail(view, outboundId);
    }

    /**
     * 항목별 잔여 수량을 FEFO 후보 행에 순서대로 채운다. 한 항목이 여러 행에 나뉠 수 있다.
     * 한 항목이라도 못 채우면 부족한 SKU를 모아 INSUFFICIENT_STOCK으로 전체 실패한다.
     */
    private List<Placement> placeByFefo(Long warehouseId, List<StoreOrderLine> targets) {
        Map<Long, List<FefoStockCandidate>> candidatesBySku = outboundQueryRepository
                .findFefoCandidates(warehouseId, targets.stream().map(StoreOrderLine::getSkuId).distinct().toList())
                .stream()
                .collect(Collectors.groupingBy(FefoStockCandidate::skuId, LinkedHashMap::new, Collectors.toList()));

        List<Placement> placements = new ArrayList<>();
        List<String> shortages = new ArrayList<>();
        Map<Long, Long> availableByLot = new HashMap<>();
        for (StoreOrderLine line : targets) {
            long remaining = line.unallocatedQuantity();
            long requested = remaining;
            for (FefoStockCandidate candidate : candidatesBySku.getOrDefault(line.getSkuId(), List.of())) {
                if (remaining == 0) {
                    break;
                }
                long available = availableByLot.getOrDefault(candidate.inventoryLotId(), candidate.availableQuantity());
                long take = Math.min(available, remaining);
                if (take <= 0) {
                    continue;
                }
                placements.add(new Placement(line.getStoreOrderLineId(), candidate.inventoryLotId(), take));
                availableByLot.put(candidate.inventoryLotId(), available - take);
                remaining -= take;
            }
            if (remaining > 0) {
                shortages.add("skuId=" + line.getSkuId() + " 요청 " + requested + ", 가용 " + (requested - remaining));
            }
        }
        if (!shortages.isEmpty()) {
            throw new BusinessException(OutboundErrorCode.INSUFFICIENT_STOCK,
                    OutboundErrorCode.INSUFFICIENT_STOCK.getDefaultMessage() + " (" + String.join("; ", shortages) + ")");
        }
        return placements;
    }

    /** 키 오름차순으로 수량을 합산한다. 재고 행·발주 항목을 ID 오름차순으로 잠그기 위한 순서다. */
    private Map<Long, Long> sumBy(List<Placement> placements, java.util.function.Function<Placement, Long> key) {
        Map<Long, Long> sums = new TreeMap<>();
        placements.forEach(p -> sums.merge(key.apply(p), p.quantity(), Long::sum));
        return sums;
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청 사용자는 필수입니다.");
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Placement(Long storeOrderLineId, Long inventoryLotId, long quantity) {
    }
}

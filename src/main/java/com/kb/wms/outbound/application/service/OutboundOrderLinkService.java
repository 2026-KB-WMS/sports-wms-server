package com.kb.wms.outbound.application.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;


import com.kb.wms.outbound.application.port.in.OutboundOrderLinkUseCase;
import com.kb.wms.outbound.application.port.in.result.OrderFulfillmentCancelResult;
import com.kb.wms.outbound.application.port.in.result.OrderOutboundSummary;
import com.kb.wms.outbound.application.port.out.OutboundRepository;
import com.kb.wms.outbound.application.port.out.OutboundStockPort;
import com.kb.wms.outbound.application.port.out.OutboundStockPort.StockQuantity;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.application.port.out.StoreOrderPort;
import com.kb.wms.outbound.application.port.out.StoreOrderPort.LineQuantity;
import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.outbound.domain.enums.OutboundHistoryEntityType;

import lombok.RequiredArgsConstructor;

/**
 * 지점 발주 도메인에 출고·재고 할당 조회와 정리를 제공한다. 발주와 출고는 서로의 엔티티를 참조하지 않고
 * ID로만 연결하며(ADR-005), 상태는 이름 문자열로 내보낸다.
 */
@Service
@RequiredArgsConstructor
public class OutboundOrderLinkService implements OutboundOrderLinkUseCase {

    /** 발주 취소에 따른 자동 처리의 상태 이력 사유. */
    static final String AUTO_CANCEL_REASON = "발주 취소로 인한 자동 처리";
    /** 발주 부분 종결에 따른 남은 할당 자동 해제의 상태 이력 사유. */
    static final String AUTO_COMPLETE_PARTIAL_REASON = "발주 부분 종결로 인한 자동 해제";

    private static final Set<OutboundStatus> PICKING_STARTED = EnumSet.of(
            OutboundStatus.PICKING, OutboundStatus.PICKED, OutboundStatus.SHIPPED, OutboundStatus.DELIVERED);
    private static final Set<OutboundStatus> IN_PROGRESS = EnumSet.of(
            OutboundStatus.READY, OutboundStatus.PICKING, OutboundStatus.PICKED, OutboundStatus.SHIPPED);

    private final OutboundRepository outboundRepository;
    private final StockAllocationRepository stockAllocationRepository;
    private final OutboundStockPort outboundStockPort;
    private final StoreOrderPort storeOrderPort;
    private final StatusHistoryUseCase statusHistoryUseCase;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findLatestOutboundStatus(Long storeOrderId) {
        List<Outbound> outbounds = outboundRepository.findByStoreOrderId(storeOrderId);
        if (outbounds.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(outbounds.get(outbounds.size() - 1).getStatus().name());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> findLatestOutboundStatuses(Collection<Long> storeOrderIds) {
        Map<Long, String> latest = new HashMap<>();
        // 생성 순(출고 ID 오름차순)이라 같은 발주의 뒤 출고가 앞 출고를 덮어쓴다.
        for (Outbound outbound : outboundRepository.findByStoreOrderIds(storeOrderIds)) {
            latest.put(outbound.getStoreOrderId(), outbound.getStatus().name());
        }
        return latest;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderOutboundSummary> findOutbounds(Long storeOrderId) {
        return outboundRepository.findByStoreOrderId(storeOrderId).stream()
                .map(o -> new OrderOutboundSummary(o.getOutboundId(), o.getOutboundNo(), o.getStatus().name(),
                        o.getShippedAt(), o.getDeliveredAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsPickingStarted(Long storeOrderId) {
        return outboundRepository.existsByStoreOrderIdAndStatusIn(storeOrderId, PICKING_STARTED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInProgressOutbound(Long storeOrderId) {
        return outboundRepository.existsByStoreOrderIdAndStatusIn(storeOrderId, IN_PROGRESS);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveFulfillment(Long storeOrderId) {
        return stockAllocationRepository.existsByStoreOrderIdAndStatus(storeOrderId, AllocationStatus.ALLOCATED)
                || outboundRepository.existsNotCanceledByStoreOrderId(storeOrderId);
    }

    /**
     * READY 출고를 CANCELED로, ALLOCATED 할당을 RELEASED로 바꾸고 재고 행과 발주 항목의 할당 수량을 줄인다.
     * 호출한 발주 서비스의 트랜잭션에 참여하며, 발주 헤더는 호출 쪽이 이미 잠근 상태다.
     * 출고를 먼저 취소하므로 그 출고에 묶여 있던 할당도 함께 해제된다.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public OrderFulfillmentCancelResult cancelFulfillment(Long storeOrderId, Long changedBy) {
        LocalDateTime now = LocalDateTime.now();

        List<Outbound> readyOutbounds = outboundRepository
                .findByStoreOrderIdAndStatusForUpdate(storeOrderId, OutboundStatus.READY);
        for (Outbound outbound : readyOutbounds) {
            outbound.cancel();
            outboundRepository.save(outbound);
            statusHistoryUseCase.record(OutboundHistoryEntityType.OUTBOUND.name(), outbound.getOutboundId(),
                    OutboundStatus.READY.name(), OutboundStatus.CANCELED.name(), AUTO_CANCEL_REASON, changedBy);
        }

        int released = releaseAllocated(storeOrderId, AUTO_CANCEL_REASON, changedBy, now);

        return new OrderFulfillmentCancelResult(released, readyOutbounds.size());
    }

    /**
     * 발주 부분 종결 때 출고에 묶이지 않고 남은 ALLOCATED 할당을 모두 RELEASED로 바꾸고
     * 재고 행과 발주 항목의 할당 수량을 줄인다. 출고는 건드리지 않는다(진행 중 출고가 없음은 호출 쪽 가드가 보장).
     * 호출한 발주 서비스의 트랜잭션에 참여하며, 발주 헤더는 호출 쪽이 이미 잠근 상태다.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int releaseUnlinkedAllocations(Long storeOrderId, Long changedBy) {
        return releaseAllocated(storeOrderId, AUTO_COMPLETE_PARTIAL_REASON, changedBy, LocalDateTime.now());
    }

    /** 발주의 ALLOCATED 할당을 잠그고 해제한다. 할당 → 발주 항목 순으로 잠그고, 재고 행은 재고 서비스가 잠근다. */
    private int releaseAllocated(Long storeOrderId, String reason, Long changedBy, LocalDateTime now) {
        List<StockAllocation> allocations = stockAllocationRepository
                .findByStoreOrderIdAndStatusForUpdate(storeOrderId, AllocationStatus.ALLOCATED);
        if (allocations.isEmpty()) {
            return 0;
        }
        Map<Long, Long> quantityByLot = new TreeMap<>();
        Map<Long, Long> quantityByLine = new TreeMap<>();
        for (StockAllocation allocation : allocations) {
            allocation.release(now);
            stockAllocationRepository.save(allocation);
            statusHistoryUseCase.record(OutboundHistoryEntityType.STOCK_ALLOCATION.name(), allocation.getAllocationId(),
                    AllocationStatus.ALLOCATED.name(), AllocationStatus.RELEASED.name(), reason, changedBy);
            quantityByLot.merge(allocation.getInventoryLotId(), allocation.getAllocatedQuantity(), Long::sum);
            quantityByLine.merge(allocation.getStoreOrderLineId(), allocation.getAllocatedQuantity(), Long::sum);
        }
        outboundStockPort.release(quantityByLot.entrySet().stream()
                .map(e -> new StockQuantity(e.getKey(), e.getValue()))
                .toList());
        storeOrderPort.decreaseAllocated(quantityByLine.entrySet().stream()
                .map(e -> new LineQuantity(e.getKey(), e.getValue()))
                .toList());
        return allocations.size();
    }
}

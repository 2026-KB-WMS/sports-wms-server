package com.kb.wms.storeorder.adapter.out.outbound;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderFulfillmentCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderOutboundView;
import com.kb.wms.storeorder.application.port.out.StoreOrderOutboundPort;
import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;

/**
 * 출고 도메인이 구현되기 전까지 쓰는 임시 연동 어댑터. 항상 "출고도 재고 할당도 없음"을 돌려준다.
 * 그래서 진행 중 출고·피킹 시작 검사는 모두 통과하고, 승인 후 취소 때 할당 해제·READY 출고 취소는 수행되지 않는다.
 * TODO: 출고 도메인(Outbound, StockAllocation) 구현 때 실제 어댑터로 교체한다. 보류 항목은 Notion "[보류]"에 기록되어 있다.
 */
@Component
public class TemporaryStoreOrderOutboundAdapter implements StoreOrderOutboundPort {

    @Override
    public Optional<StoreOrderOutboundStatus> findLatestOutboundStatus(Long storeOrderId) {
        return Optional.empty();
    }

    @Override
    public Map<Long, StoreOrderOutboundStatus> findLatestOutboundStatuses(Collection<Long> storeOrderIds) {
        return Map.of();
    }

    @Override
    public List<StoreOrderOutboundView> findOutbounds(Long storeOrderId) {
        return List.of();
    }

    @Override
    public boolean existsPickingStarted(Long storeOrderId) {
        return false;
    }

    @Override
    public boolean existsInProgressOutbound(Long storeOrderId) {
        return false;
    }

    @Override
    public boolean existsActiveFulfillment(Long storeOrderId) {
        return false;
    }

    @Override
    public StoreOrderFulfillmentCancelResult cancelFulfillment(Long storeOrderId, Long changedBy) {
        return StoreOrderFulfillmentCancelResult.NONE;
    }
}

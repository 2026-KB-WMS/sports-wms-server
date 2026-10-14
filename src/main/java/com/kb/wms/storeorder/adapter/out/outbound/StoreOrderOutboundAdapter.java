package com.kb.wms.storeorder.adapter.out.outbound;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.outbound.application.port.in.OutboundOrderLinkUseCase;
import com.kb.wms.outbound.application.port.in.result.OrderFulfillmentCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderFulfillmentCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderOutboundView;
import com.kb.wms.storeorder.application.port.out.StoreOrderOutboundPort;
import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;

import lombok.RequiredArgsConstructor;

/**
 * 지점 발주의 출고 연동 포트를 출고 도메인의 유스케이스에 연결한다. 출고가 내보낸 상태 이름과 결과를
 * 이 도메인의 타입으로 바꾸는 유일한 곳이다.
 */
@Component
@RequiredArgsConstructor
public class StoreOrderOutboundAdapter implements StoreOrderOutboundPort {

    private final OutboundOrderLinkUseCase outboundOrderLinkUseCase;

    @Override
    public Optional<StoreOrderOutboundStatus> findLatestOutboundStatus(Long storeOrderId) {
        return outboundOrderLinkUseCase.findLatestOutboundStatus(storeOrderId).map(StoreOrderOutboundStatus::valueOf);
    }

    @Override
    public Map<Long, StoreOrderOutboundStatus> findLatestOutboundStatuses(Collection<Long> storeOrderIds) {
        Map<Long, StoreOrderOutboundStatus> statuses = new HashMap<>();
        outboundOrderLinkUseCase.findLatestOutboundStatuses(storeOrderIds)
                .forEach((storeOrderId, status) -> statuses.put(storeOrderId, StoreOrderOutboundStatus.valueOf(status)));
        return statuses;
    }

    @Override
    public List<StoreOrderOutboundView> findOutbounds(Long storeOrderId) {
        return outboundOrderLinkUseCase.findOutbounds(storeOrderId).stream()
                .map(o -> new StoreOrderOutboundView(o.outboundId(), o.outboundNo(), o.status(),
                        o.shippedAt(), o.deliveredAt()))
                .toList();
    }

    @Override
    public boolean existsPickingStarted(Long storeOrderId) {
        return outboundOrderLinkUseCase.existsPickingStarted(storeOrderId);
    }

    @Override
    public boolean existsInProgressOutbound(Long storeOrderId) {
        return outboundOrderLinkUseCase.existsInProgressOutbound(storeOrderId);
    }

    @Override
    public boolean existsActiveFulfillment(Long storeOrderId) {
        return outboundOrderLinkUseCase.existsActiveFulfillment(storeOrderId);
    }

    @Override
    public StoreOrderFulfillmentCancelResult cancelFulfillment(Long storeOrderId, Long changedBy) {
        OrderFulfillmentCancelResult result = outboundOrderLinkUseCase.cancelFulfillment(storeOrderId, changedBy);
        return new StoreOrderFulfillmentCancelResult(result.releasedAllocationCount(), result.canceledOutboundCount());
    }

    @Override
    public int releaseUnlinkedAllocations(Long storeOrderId, Long changedBy) {
        return outboundOrderLinkUseCase.releaseUnlinkedAllocations(storeOrderId, changedBy);
    }
}

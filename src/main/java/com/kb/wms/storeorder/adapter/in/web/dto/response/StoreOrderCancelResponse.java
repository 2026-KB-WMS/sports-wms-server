package com.kb.wms.storeorder.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderCancelResult;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * PATCH /api/v1/orders/{orderId}/cancel 응답.
 * 출고 도메인 연동 전에는 releasedAllocationCount·canceledOutboundCount가 항상 0이다.
 */
public record StoreOrderCancelResponse(
        Long storeOrderId,
        String orderNo,
        StoreOrderStatus status,
        String statusReason,
        int releasedAllocationCount,
        int canceledOutboundCount,
        LocalDateTime updatedAt
) {

    public static StoreOrderCancelResponse from(StoreOrderCancelResult result) {
        return new StoreOrderCancelResponse(
                result.storeOrderId(), result.orderNo(), result.status(), result.statusReason(),
                result.releasedAllocationCount(), result.canceledOutboundCount(), result.updatedAt());
    }
}

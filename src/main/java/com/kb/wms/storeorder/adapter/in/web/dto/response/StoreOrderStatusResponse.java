package com.kb.wms.storeorder.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderStatusChange;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * PATCH /api/v1/orders/{orderId}/reject, hold, resume 응답.
 * 재개(resume)는 재개 후 ASSIGNED의 statusReason이 null이다.
 */
public record StoreOrderStatusResponse(
        Long storeOrderId,
        String orderNo,
        StoreOrderStatus status,
        String statusReason,
        LocalDateTime updatedAt
) {

    public static StoreOrderStatusResponse from(StoreOrderStatusChange change) {
        return new StoreOrderStatusResponse(
                change.storeOrderId(), change.orderNo(), change.status(), change.statusReason(), change.updatedAt());
    }
}

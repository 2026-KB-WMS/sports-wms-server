package com.kb.wms.storeorder.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderStatusChange;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * PATCH /api/v1/orders/{orderId}/approve 응답. 승인은 사유가 없어 statusReason을 내려주지 않는다.
 */
public record StoreOrderApproveResponse(
        Long storeOrderId,
        String orderNo,
        StoreOrderStatus status,
        LocalDateTime updatedAt
) {

    public static StoreOrderApproveResponse from(StoreOrderStatusChange change) {
        return new StoreOrderApproveResponse(
                change.storeOrderId(), change.orderNo(), change.status(), change.updatedAt());
    }
}

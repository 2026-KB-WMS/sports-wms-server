package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * PATCH /api/v1/purchase-orders/{purchaseOrderId}/confirm, /cancel (상태 전이) 응답.
 * 취소 사유(cancelReason)는 StatusHistory 도입 후 취소 응답에 추가한다.
 */
public record PurchaseOrderStatusResponse(
        Long purchaseOrderId,
        String purchaseOrderNo,
        PurchaseOrderStatus status,
        LocalDateTime updatedAt
) {

    public static PurchaseOrderStatusResponse from(PurchaseOrder purchaseOrder) {
        return new PurchaseOrderStatusResponse(
                purchaseOrder.getPurchaseOrderId(),
                purchaseOrder.getPurchaseOrderNo(),
                purchaseOrder.getStatus(),
                purchaseOrder.getUpdatedAt());
    }
}

package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * PATCH /api/v1/purchase-orders/{purchaseOrderId}/confirm, /cancel (상태 전이) 응답.
 * 취소 사유(cancelReason)는 취소 응답에서만 채운다(확정 응답과 사유 없이 취소한 경우는 null).
 */
public record PurchaseOrderStatusResponse(
        Long purchaseOrderId,
        String purchaseOrderNo,
        PurchaseOrderStatus status,
        String cancelReason,
        LocalDateTime updatedAt
) {

    public static PurchaseOrderStatusResponse from(PurchaseOrder purchaseOrder) {
        return of(purchaseOrder, null);
    }

    public static PurchaseOrderStatusResponse of(PurchaseOrder purchaseOrder, String cancelReason) {
        return new PurchaseOrderStatusResponse(
                purchaseOrder.getPurchaseOrderId(),
                purchaseOrder.getPurchaseOrderNo(),
                purchaseOrder.getStatus(),
                cancelReason,
                purchaseOrder.getUpdatedAt());
    }
}

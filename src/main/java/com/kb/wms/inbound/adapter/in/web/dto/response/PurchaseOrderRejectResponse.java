package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * PATCH /api/v1/purchase-orders/{purchaseOrderId}/reject (발주 반려) 응답.
 */
public record PurchaseOrderRejectResponse(
        Long purchaseOrderId,
        String purchaseOrderNo,
        PurchaseOrderStatus status,
        String rejectReason,
        LocalDateTime updatedAt
) {

    public static PurchaseOrderRejectResponse of(PurchaseOrder purchaseOrder, String rejectReason) {
        return new PurchaseOrderRejectResponse(
                purchaseOrder.getPurchaseOrderId(),
                purchaseOrder.getPurchaseOrderNo(),
                purchaseOrder.getStatus(),
                rejectReason,
                purchaseOrder.getUpdatedAt());
    }
}

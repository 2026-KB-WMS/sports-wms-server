package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.application.port.in.result.PurchaseOrderConfirmResult;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * PATCH /api/v1/purchase-orders/{purchaseOrderId}/confirm (발주 확정) 응답: 확정된 발주와 만들어진 공급처 발주서.
 */
public record PurchaseOrderConfirmResponse(
        Long purchaseOrderId,
        String purchaseOrderNo,
        PurchaseOrderStatus status,
        LocalDateTime updatedAt,
        List<PurchaseOrderSupplierOrderResponse> supplierOrders
) {

    public static PurchaseOrderConfirmResponse from(PurchaseOrderConfirmResult result) {
        return new PurchaseOrderConfirmResponse(
                result.purchaseOrder().getPurchaseOrderId(),
                result.purchaseOrder().getPurchaseOrderNo(),
                result.purchaseOrder().getStatus(),
                result.purchaseOrder().getUpdatedAt(),
                result.supplierOrders().stream().map(PurchaseOrderSupplierOrderResponse::from).toList());
    }
}

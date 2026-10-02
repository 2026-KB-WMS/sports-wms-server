package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.util.List;

import com.kb.wms.inbound.application.port.in.result.PurchaseOrderDetails;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * GET /api/v1/purchase-orders/{purchaseOrderId}/details (발주 상세, SKU별 수량) 응답.
 */
public record PurchaseOrderDetailsResponse(
        Long purchaseOrderId,
        String purchaseOrderNo,
        PurchaseOrderStatus status,
        List<PurchaseOrderLineResponse> items
) {

    public static PurchaseOrderDetailsResponse from(PurchaseOrderDetails details) {
        return new PurchaseOrderDetailsResponse(
                details.purchaseOrderId(),
                details.purchaseOrderNo(),
                details.status(),
                details.items().stream().map(PurchaseOrderLineResponse::from).toList());
    }
}

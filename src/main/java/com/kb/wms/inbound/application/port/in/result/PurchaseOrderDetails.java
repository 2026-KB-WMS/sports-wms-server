package com.kb.wms.inbound.application.port.in.result;

import java.util.List;

import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * 발주 상세 (GET /api/v1/purchase-orders/{purchaseOrderId}/details): 발주 식별 정보 + SKU별 항목.
 */
public record PurchaseOrderDetails(
        Long purchaseOrderId,
        String purchaseOrderNo,
        PurchaseOrderStatus status,
        List<PurchaseOrderLineView> items
) {
}

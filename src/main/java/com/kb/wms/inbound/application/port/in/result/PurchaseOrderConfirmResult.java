package com.kb.wms.inbound.application.port.in.result;

import java.util.List;

import com.kb.wms.inbound.domain.entity.PurchaseOrder;

/**
 * 발주 확정 결과: 확정된 발주와 이번 확정으로 만들어진 공급처 발주서(공급처당 한 건).
 */
public record PurchaseOrderConfirmResult(
        PurchaseOrder purchaseOrder,
        List<SupplierOrderSummary> supplierOrders
) {
}

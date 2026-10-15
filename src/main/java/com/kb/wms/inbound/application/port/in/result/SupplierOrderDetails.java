package com.kb.wms.inbound.application.port.in.result;

import java.util.List;

/**
 * 공급처 발주서 상세 (GET /api/v1/supplier-orders/{supplierOrderId}): 발주서 헤더 + SKU별 항목.
 */
public record SupplierOrderDetails(
        SupplierOrderView header,
        List<SupplierOrderLineView> lines
) {
}

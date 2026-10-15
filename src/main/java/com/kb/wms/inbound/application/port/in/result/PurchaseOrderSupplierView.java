package com.kb.wms.inbound.application.port.in.result;

/**
 * 발주(요청)의 라인에 배정된 공급처 한 곳. 목록 응답의 suppliers[]를 만드는 데 쓴다.
 */
public record PurchaseOrderSupplierView(
        Long purchaseOrderId,
        Long supplierId,
        String supplierName
) {
}

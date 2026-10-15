package com.kb.wms.inbound.adapter.in.web.dto.response;

import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSupplierView;

/**
 * 발주 목록 항목에 담기는 공급처 한 곳(라인에 배정된 공급처).
 */
public record PurchaseOrderSupplierResponse(
        Long supplierId,
        String supplierName
) {

    public static PurchaseOrderSupplierResponse from(PurchaseOrderSupplierView supplier) {
        return new PurchaseOrderSupplierResponse(supplier.supplierId(), supplier.supplierName());
    }
}

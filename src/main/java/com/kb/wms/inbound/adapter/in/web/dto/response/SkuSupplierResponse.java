package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inbound.application.port.in.result.SkuSupplierView;

/**
 * SKU에 매핑된 공급처 응답(목록 항목, 등록 응답 공통).
 */
public record SkuSupplierResponse(
        Long supplierId,
        String supplierCode,
        String supplierName,
        boolean isActive,
        LocalDateTime createdAt
) {

    public static SkuSupplierResponse from(SkuSupplierView view) {
        return new SkuSupplierResponse(
                view.supplierId(), view.supplierCode(), view.supplierName(), view.isActive(), view.createdAt());
    }
}

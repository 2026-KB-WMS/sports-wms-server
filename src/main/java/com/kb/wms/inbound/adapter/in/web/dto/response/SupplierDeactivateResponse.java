package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.entity.Supplier;

/**
 * PATCH /api/v1/suppliers/{supplierId}/deactivate 응답.
 */
public record SupplierDeactivateResponse(
        Long supplierId,
        String supplierCode,
        String supplierName,
        boolean isActive,
        LocalDateTime updatedAt
) {

    public static SupplierDeactivateResponse from(Supplier supplier) {
        return new SupplierDeactivateResponse(
                supplier.getSupplierId(),
                supplier.getSupplierCode(),
                supplier.getName(),
                supplier.isActive(),
                supplier.getUpdatedAt());
    }
}

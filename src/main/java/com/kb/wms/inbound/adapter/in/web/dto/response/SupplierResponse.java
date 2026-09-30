package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.entity.Supplier;

/**
 * POST, GET, PATCH /api/v1/suppliers/{supplierId}(및 등록/수정) 응답.
 */
public record SupplierResponse(
        Long supplierId,
        String supplierCode,
        String supplierName,
        String managerName,
        String contactNumber,
        String email,
        String address,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getSupplierId(),
                supplier.getSupplierCode(),
                supplier.getName(),
                supplier.getManagerName(),
                supplier.getContactNumber(),
                supplier.getEmail(),
                supplier.getAddress(),
                supplier.isActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt());
    }
}

package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;

import com.kb.wms.inbound.domain.entity.Supplier;

/**
 * GET /api/v1/suppliers (전체 공급처 조회) 목록 항목 응답.
 */
public record SupplierSummaryResponse(
        Long supplierId,
        String supplierCode,
        String supplierName,
        String managerName,
        String contactNumber,
        String email,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean isActive
) {

    public static SupplierSummaryResponse from(Supplier supplier) {
        return new SupplierSummaryResponse(
                supplier.getSupplierId(),
                supplier.getSupplierCode(),
                supplier.getName(),
                supplier.getManagerName(),
                supplier.getContactNumber(),
                supplier.getEmail(),
                supplier.getAddress(),
                supplier.getLatitude(),
                supplier.getLongitude(),
                supplier.isActive());
    }
}

package com.kb.wms.warehouse.adapter.in.web.dto.response;

import java.math.BigDecimal;

import com.kb.wms.warehouse.domain.entity.Warehouse;

/**
 * GET /api/v1/warehouses (전체 창고 조회) 목록 항목 응답.
 */
public record WarehouseSummaryResponse(
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String address,
        String contactNumber,
        BigDecimal totalCapacity,
        boolean isActive
) {

    public static WarehouseSummaryResponse from(Warehouse warehouse) {
        return new WarehouseSummaryResponse(
                warehouse.getWarehouseId(),
                warehouse.getWarehouseCode(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getContactNumber(),
                warehouse.getTotalCapacity(),
                warehouse.isActive());
    }
}

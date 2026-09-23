package com.kb.wms.warehouse.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.warehouse.domain.entity.Warehouse;

/**
 * POST, GET, PATCH /api/v1/warehouses/{warehouseId}(및 하위 등록/수정/비활성화) 응답.
 */
public record WarehouseResponse(
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String address,
        String contactNumber,
        BigDecimal totalCapacity,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getWarehouseId(),
                warehouse.getWarehouseCode(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getContactNumber(),
                warehouse.getTotalCapacity(),
                warehouse.isActive(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt());
    }
}

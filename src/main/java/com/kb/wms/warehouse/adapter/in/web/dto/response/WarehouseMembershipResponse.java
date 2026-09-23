package com.kb.wms.warehouse.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.warehouse.application.port.in.result.WarehouseMembershipSummary;

/**
 * GET /api/v1/warehouses/my 응답 항목.
 */
public record WarehouseMembershipResponse(
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String address,
        String contactNumber,
        BigDecimal totalCapacity,
        boolean isActive,
        Long warehouseMemberId,
        String memberRole,
        LocalDateTime assignedAt
) {

    public static WarehouseMembershipResponse from(WarehouseMembershipSummary summary) {
        return new WarehouseMembershipResponse(
                summary.warehouseId(),
                summary.warehouseCode(),
                summary.name(),
                summary.address(),
                summary.contactNumber(),
                summary.totalCapacity(),
                summary.isActive(),
                summary.warehouseMemberId(),
                summary.memberRole(),
                summary.assignedAt());
    }
}

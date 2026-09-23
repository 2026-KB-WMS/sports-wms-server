package com.kb.wms.warehouse.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GET /api/v1/warehouses/my 응답에 필요한, 창고 정보와 배정(WarehouseMember) 정보를 함께 담은 요약.
 */
public record WarehouseMembershipSummary(
        Long warehouseId,
        String warehouseCode,
        String name,
        String address,
        String contactNumber,
        BigDecimal totalCapacity,
        boolean isActive,
        Long warehouseMemberId,
        String memberRole,
        LocalDateTime assignedAt
) {
}

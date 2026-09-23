package com.kb.wms.warehouse.adapter.in.web.dto.response;

/**
 * DELETE /api/v1/warehouses/managers/{warehouseMemberId} 응답.
 */
public record WarehouseMemberReleaseResponse(
        Long warehouseMemberId
) {
}

package com.kb.wms.warehouse.adapter.in.web.dto.response;

/**
 * DELETE /api/v1/warehouses/sections/{sectionId} 응답.
 */
public record WarehouseSectionDeleteResponse(
        Long sectionId
) {
}

package com.kb.wms.inventory.adapter.in.web.dto.response;

import com.kb.wms.inventory.application.port.in.result.InventorySectionSummary;

/**
 * GET /api/v1/inventory/by-section 목록 항목 (구역 × SKU 기준 집계).
 */
public record InventorySectionSummaryResponse(
        Long warehouseId,
        String warehouseName,
        Long sectionId,
        String sectionCode,
        String sectionName,
        Long skuId,
        String skuCode,
        String skuName,
        String unit,
        Long categoryId,
        String categoryName,
        Long totalQuantity,
        Long availableQuantity,
        Long allocatedQuantity,
        Long defectiveQuantity
) {

    public static InventorySectionSummaryResponse from(InventorySectionSummary summary) {
        return new InventorySectionSummaryResponse(
                summary.warehouseId(),
                summary.warehouseName(),
                summary.sectionId(),
                summary.sectionCode(),
                summary.sectionName(),
                summary.skuId(),
                summary.skuCode(),
                summary.skuName(),
                summary.unit(),
                summary.categoryId(),
                summary.categoryName(),
                summary.totalQuantity(),
                summary.availableQuantity(),
                summary.allocatedQuantity(),
                summary.defectiveQuantity());
    }
}

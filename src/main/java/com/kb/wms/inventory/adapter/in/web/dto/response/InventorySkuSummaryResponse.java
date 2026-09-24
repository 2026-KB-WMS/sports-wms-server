package com.kb.wms.inventory.adapter.in.web.dto.response;

import com.kb.wms.inventory.application.port.in.result.InventorySkuSummary;

/**
 * GET /api/v1/inventory 목록 항목 (SKU 기준 집계).
 */
public record InventorySkuSummaryResponse(
        Long skuId,
        String skuCode,
        String skuName,
        String unit,
        Long totalQuantity,
        Long availableQuantity,
        Long allocatedQuantity,
        Long defectiveQuantity
) {

    public static InventorySkuSummaryResponse from(InventorySkuSummary summary) {
        return new InventorySkuSummaryResponse(
                summary.skuId(),
                summary.skuCode(),
                summary.skuName(),
                summary.unit(),
                summary.totalQuantity(),
                summary.availableQuantity(),
                summary.allocatedQuantity(),
                summary.defectiveQuantity());
    }
}

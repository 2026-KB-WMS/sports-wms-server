package com.kb.wms.inventory.application.port.in.result;

/**
 * 구역 × SKU 기준 재고 집계 (GET /api/v1/inventory/by-section).
 * 수량 집계 기준은 {@link InventorySkuSummary}와 같다.
 */
public record InventorySectionSummary(
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
}

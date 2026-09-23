package com.kb.wms.inventory.application.port.in.query;

/**
 * GET /api/v1/inventory (SKU 기준 집계) 검색 조건.
 *
 * @param keyword SKU 코드·SKU명 부분 일치
 */
public record InventorySearchCondition(
        Long skuId,
        Long warehouseId,
        String keyword
) {
}

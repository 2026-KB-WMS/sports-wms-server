package com.kb.wms.inventory.application.port.in.query;

/**
 * GET /api/v1/inventory/low-stock 검색 조건.
 *
 * @param warehouseId 생략하면 모든 창고의 가용 재고를 합산해 비교
 * @param keyword     SKU 코드·SKU명 부분 일치
 */
public record LowStockSearchCondition(
        Long warehouseId,
        String keyword
) {
}

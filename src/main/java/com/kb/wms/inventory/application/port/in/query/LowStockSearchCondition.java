package com.kb.wms.inventory.application.port.in.query;

import java.util.List;

/**
 * GET /api/v1/inventory/low-stock 검색 조건.
 *
 * @param warehouseId 생략하면 모든 창고의 가용 재고를 합산해 비교
 * @param keyword      SKU 코드·SKU명 부분 일치
 * @param warehouseIds 조회를 허용할 창고 범위(담당 창고). null이면 제한 없음
 */
public record LowStockSearchCondition(
        Long warehouseId,
        String keyword,
        List<Long> warehouseIds
) {
    public LowStockSearchCondition(Long warehouseId, String keyword) {
        this(warehouseId, keyword, null);
    }
}

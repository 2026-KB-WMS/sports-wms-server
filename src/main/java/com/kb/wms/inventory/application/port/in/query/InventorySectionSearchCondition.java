package com.kb.wms.inventory.application.port.in.query;

import java.util.List;

/**
 * GET /api/v1/inventory/by-section (구역 × SKU 기준 집계) 검색 조건.
 *
 * @param categoryId   SKU 상품의 카테고리. 하위 카테고리는 포함하지 않는다
 * @param keyword      SKU 코드·SKU명 부분 일치
 * @param warehouseIds 조회를 허용할 창고 범위(담당 창고). null이면 제한 없음
 */
public record InventorySectionSearchCondition(
        Long warehouseId,
        Long sectionId,
        Long categoryId,
        Long skuId,
        String keyword,
        List<Long> warehouseIds
) {
    /** 담당 창고 범위 제한 없이 조회하는 조건. 사용자 요청에서는 서비스가 범위를 채워 넘긴다. */
    public static InventorySectionSearchCondition unscoped(
            Long warehouseId, Long sectionId, Long categoryId, Long skuId, String keyword) {
        return new InventorySectionSearchCondition(warehouseId, sectionId, categoryId, skuId, keyword, null);
    }
}

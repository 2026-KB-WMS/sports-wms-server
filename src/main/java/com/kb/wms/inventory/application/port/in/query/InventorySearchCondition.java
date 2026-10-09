package com.kb.wms.inventory.application.port.in.query;

import java.util.List;

/**
 * GET /api/v1/inventory (SKU 기준 집계) 검색 조건.
 *
 * @param keyword      SKU 코드·SKU명 부분 일치
 * @param warehouseIds 조회를 허용할 창고 범위(담당 창고). null이면 제한 없음
 */
public record InventorySearchCondition(
        Long skuId,
        Long warehouseId,
        String keyword,
        List<Long> warehouseIds
) {
    /** 담당 창고·지점 범위 제한 없이 조회하는 조건. 사용자 요청에서는 서비스가 범위를 채워 넘긴다. */
    public static InventorySearchCondition unscoped(Long skuId, Long warehouseId, String keyword) {
        return new InventorySearchCondition(skuId, warehouseId, keyword, null);
    }
}

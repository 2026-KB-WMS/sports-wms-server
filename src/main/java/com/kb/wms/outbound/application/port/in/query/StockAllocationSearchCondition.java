package com.kb.wms.outbound.application.port.in.query;

import java.util.List;

import com.kb.wms.outbound.domain.enums.AllocationStatus;

/**
 * GET /api/v1/allocations 검색 조건. null은 조건 없음이다.
 *
 * @param keyword      발주 번호·SKU 코드·로트 번호 부분 일치
 * @param warehouseIds 조회를 허용할 창고 범위(담당 창고). null이면 제한 없음
 */
public record StockAllocationSearchCondition(
        Long storeOrderId,
        Long warehouseId,
        Long skuId,
        AllocationStatus status,
        String keyword,
        List<Long> warehouseIds
) {

    /** 담당 창고·지점 범위 제한 없이 조회하는 조건. 사용자 요청에서는 서비스가 범위를 채워 넘긴다. */
    public static StockAllocationSearchCondition unscoped(Long storeOrderId, Long warehouseId, Long skuId, AllocationStatus status,
            String keyword) {
        return new StockAllocationSearchCondition(storeOrderId, warehouseId, skuId, status, keyword, null);
    }
}
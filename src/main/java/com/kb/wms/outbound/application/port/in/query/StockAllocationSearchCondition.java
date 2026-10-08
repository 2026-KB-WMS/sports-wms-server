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

    public StockAllocationSearchCondition(Long storeOrderId, Long warehouseId, Long skuId, AllocationStatus status,
                                          String keyword) {
        this(storeOrderId, warehouseId, skuId, status, keyword, null);
    }
}
package com.kb.wms.outbound.application.port.in.query;

import com.kb.wms.outbound.domain.enums.AllocationStatus;

/**
 * GET /api/v1/allocations 검색 조건. null은 조건 없음이다.
 *
 * @param keyword 발주 번호·SKU 코드·로트 번호 부분 일치
 */
public record StockAllocationSearchCondition(
        Long storeOrderId,
        Long warehouseId,
        Long skuId,
        AllocationStatus status,
        String keyword
) {
}
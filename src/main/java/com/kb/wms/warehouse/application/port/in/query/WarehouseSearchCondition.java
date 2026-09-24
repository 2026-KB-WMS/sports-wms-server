package com.kb.wms.warehouse.application.port.in.query;

/**
 * GET /api/v1/warehouses 검색 조건. null인 조건은 무시한다.
 *
 * @param keyword  창고명·창고 코드 부분 일치
 * @param isActive 운영 상태 필터 (null이면 전체)
 */
public record WarehouseSearchCondition(
        String keyword,
        Boolean isActive
) {
}

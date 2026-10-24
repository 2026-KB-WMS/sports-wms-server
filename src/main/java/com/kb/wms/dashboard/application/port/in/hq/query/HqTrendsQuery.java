package com.kb.wms.dashboard.application.port.in.hq.query;

/**
 * GET /api/v1/dashboard/hq/trends 조회 조건. null이면 서비스가 기본값을 채운다.
 *
 * @param months      조회할 달 수, 1~12 (기본 6)
 * @param warehouseId 창고 필터. 없는 창고면 WAREHOUSE_NOT_FOUND
 */
public record HqTrendsQuery(
        Integer months,
        Long warehouseId
) {
}

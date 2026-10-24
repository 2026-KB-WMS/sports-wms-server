package com.kb.wms.dashboard.application.port.in.warehouse.query;

/**
 * GET /api/v1/dashboard/warehouse/summary 조회 조건. null이면 서비스가 기본값을 채운다.
 *
 * @param warehouseId        조회할 창고. 담당 창고가 하나면 생략 가능, 여럿이면 필수
 * @param expiringWithinDays 유통기한 임박 기준 일수, 1~365 (기본 30)
 */
public record WarehouseSummaryQuery(
        Long warehouseId,
        Integer expiringWithinDays
) {
}

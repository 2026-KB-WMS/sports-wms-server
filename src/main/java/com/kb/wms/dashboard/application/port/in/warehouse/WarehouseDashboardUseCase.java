package com.kb.wms.dashboard.application.port.in.warehouse;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.dashboard.application.port.in.warehouse.query.WarehouseSummaryQuery;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseDashboardSummary;

/**
 * 창고 관리자 홈 대시보드 조회 유스케이스(읽기 전용).
 * GET /api/v1/dashboard/warehouse/summary
 */
public interface WarehouseDashboardUseCase {

    /**
     * 담당 창고의 작업 대기, 수용량, 재고 주의 항목.
     * 담당 창고가 여럿인데 warehouseId가 없거나 expiringWithinDays가 범위를 벗어나면 400 VALIDATION_ERROR,
     * 담당이 아닌 창고면 403 FORBIDDEN.
     */
    WarehouseDashboardSummary getSummary(WarehouseSummaryQuery query, AuthenticatedUser principal);
}

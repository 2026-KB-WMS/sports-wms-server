package com.kb.wms.dashboard.application.port.in.hq;

import com.kb.wms.dashboard.application.port.in.hq.query.HqSummaryQuery;
import com.kb.wms.dashboard.application.port.in.hq.query.HqTrendsQuery;
import com.kb.wms.dashboard.application.port.in.hq.result.HqDashboardSummary;
import com.kb.wms.dashboard.application.port.in.hq.result.HqDashboardTrends;

/**
 * 본사 홈 대시보드 조회 유스케이스(읽기 전용).
 * GET /api/v1/dashboard/hq/summary, GET /api/v1/dashboard/hq/trends
 */
public interface HqDashboardUseCase {

    /** 조치 대기와 총 집계. 입력 범위를 벗어나면 400 VALIDATION_ERROR. */
    HqDashboardSummary getSummary(HqSummaryQuery query);

    /** 월간 추이와 출고 상위 SKU. 입력 범위를 벗어나면 400 VALIDATION_ERROR, 없는 창고면 404 WAREHOUSE_NOT_FOUND. */
    HqDashboardTrends getTrends(HqTrendsQuery query);
}

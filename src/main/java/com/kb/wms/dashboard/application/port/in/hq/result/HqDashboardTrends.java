package com.kb.wms.dashboard.application.port.in.hq.result;

import java.util.List;

/**
 * 본사 홈 추이 (GET /api/v1/dashboard/hq/trends).
 *
 * @param items            과거 달부터 현재 달 순서의 월별 입출고량
 * @param topOutboundSkus  현재 달 출고 완료 수량 상위 SKU
 */
public record HqDashboardTrends(
        List<MonthlyFlow> items,
        List<TopOutboundSku> topOutboundSkus
) {
}

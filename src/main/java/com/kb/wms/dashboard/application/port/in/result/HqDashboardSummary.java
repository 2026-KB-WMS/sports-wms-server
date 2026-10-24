package com.kb.wms.dashboard.application.port.in.result;

import java.time.YearMonth;
import java.util.List;

/**
 * 본사 홈 요약 (GET /api/v1/dashboard/hq/summary).
 *
 * @param month 월 단위 집계(완료 입출고, 지점 발주)의 기준 월
 */
public record HqDashboardSummary(
        YearMonth month,
        PendingCounts pending,
        InventoryTotals inventory,
        FlowTotals inbound,
        FlowTotals outbound,
        StoreOrderStats storeOrders,
        List<WarehouseDashboardRow> warehouses
) {
}

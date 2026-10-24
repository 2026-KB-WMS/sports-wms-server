package com.kb.wms.dashboard.application.port.in.warehouse.result;

/**
 * 창고 관리자 홈 요약 (GET /api/v1/dashboard/warehouse/summary).
 */
public record WarehouseDashboardSummary(
        Long warehouseId,
        String warehouseName,
        WarehouseWorkQueue pending,
        CapacityUsage capacity,
        WarehouseInventoryAlerts inventory
) {
}

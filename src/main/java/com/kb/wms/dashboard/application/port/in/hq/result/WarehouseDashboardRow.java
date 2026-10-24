package com.kb.wms.dashboard.application.port.in.hq.result;

/**
 * 창고별 비교 한 줄.
 */
public record WarehouseDashboardRow(
        Long warehouseId,
        String warehouseName,
        Long inventoryQuantity,
        Long lowStockSkuCount,
        WarehouseFlowQuantity inbound,
        WarehouseFlowQuantity outbound
) {
}

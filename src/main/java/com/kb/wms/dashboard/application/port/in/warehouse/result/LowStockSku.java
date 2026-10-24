package com.kb.wms.dashboard.application.port.in.warehouse.result;

/**
 * 안전 재고에 못 미치는 SKU 한 줄.
 */
public record LowStockSku(
        Long skuId,
        String skuCode,
        String skuName,
        Long availableQuantity,
        Long safetyStockQuantity
) {
}

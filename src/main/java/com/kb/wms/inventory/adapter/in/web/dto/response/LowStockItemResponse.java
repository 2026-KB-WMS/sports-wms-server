package com.kb.wms.inventory.adapter.in.web.dto.response;

import com.kb.wms.inventory.application.port.in.result.LowStockItem;

/**
 * GET /api/v1/inventory/low-stock 목록 항목.
 */
public record LowStockItemResponse(
        Long skuId,
        String skuCode,
        String skuName,
        String unit,
        Long safetyStockQuantity,
        Long availableQuantity,
        Long shortageQuantity
) {

    public static LowStockItemResponse from(LowStockItem item) {
        return new LowStockItemResponse(
                item.skuId(),
                item.skuCode(),
                item.skuName(),
                item.unit(),
                item.safetyStockQuantity(),
                item.availableQuantity(),
                item.shortageQuantity());
    }
}

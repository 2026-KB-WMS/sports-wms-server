package com.kb.wms.dashboard.application.port.in.warehouse.result;

import java.util.List;

/**
 * 담당 창고의 재고 주의 항목.
 *
 * @param lowStockSkuCount 안전 재고 미만 SKU 수(GET /inventory/low-stock과 같은 조건, 이 창고 재고 기준)
 * @param lowStockSkus     부족 폭(safetyStockQuantity - availableQuantity)이 큰 상위 5개. 같으면 skuId 오름차순
 * @param expiringLotCount 이 창고에 재고가 남은 AVAILABLE 로트 중 유통기한이 임박한 로트 수
 */
public record WarehouseInventoryAlerts(
        Long lowStockSkuCount,
        List<LowStockSku> lowStockSkus,
        Long expiringLotCount
) {
}

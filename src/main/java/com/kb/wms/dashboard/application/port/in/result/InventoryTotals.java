package com.kb.wms.dashboard.application.port.in.result;

/**
 * 전 창고 재고 총량.
 *
 * @param totalQuantity    격리(QUARANTINED)·폐기(DISPOSED) 로트를 뺀 재고 수량 합
 * @param lowStockSkuCount 안전 재고 미만 SKU 수(GET /inventory/low-stock과 같은 조건)
 * @param expiringLotCount 재고가 남은 AVAILABLE 로트 중 유통기한이 임박한 로트 수
 */
public record InventoryTotals(
        Long totalQuantity,
        Long activeSkuCount,
        Long lowStockSkuCount,
        Long expiringLotCount
) {
}

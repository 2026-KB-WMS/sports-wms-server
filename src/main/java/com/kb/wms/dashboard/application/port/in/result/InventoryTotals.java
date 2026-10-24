package com.kb.wms.dashboard.application.port.in.result;

/**
 * 전 창고 재고 총량.
 *
 * @param totalQuantity       정상 재고 수량 합. 격리(QUARANTINED)·폐기(DISPOSED)·만료(EXPIRED) 로트와 불량(DEFECTIVE) 재고는 뺀다
 * @param quarantinedQuantity 격리 로트의 재고 수량 합
 * @param disposedQuantity    폐기 로트의 재고 수량 합
 * @param expiredQuantity     만료 로트의 재고 수량 합
 * @param defectiveQuantity   격리·폐기·만료 로트가 아닌 로트 중 품질 상태가 불량인 재고 수량 합
 * @param lowStockSkuCount    안전 재고 미만 SKU 수(GET /inventory/low-stock과 같은 조건)
 * @param expiringLotCount    재고가 남은 AVAILABLE 로트 중 유통기한이 임박한 로트 수
 */
public record InventoryTotals(
        Long totalQuantity,
        Long quarantinedQuantity,
        Long disposedQuantity,
        Long expiredQuantity,
        Long defectiveQuantity,
        Long activeSkuCount,
        Long lowStockSkuCount,
        Long expiringLotCount
) {
}

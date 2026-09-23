package com.kb.wms.inventory.application.port.in.result;

/**
 * SKU 기준 재고 집계 (GET /api/v1/inventory).
 *
 * @param totalQuantity     보유 수량(on_hand) 합계
 * @param availableQuantity 품질·로트 상태가 모두 AVAILABLE인 재고의 (on_hand - allocated) 합계
 * @param allocatedQuantity 할당 수량 합계
 * @param defectiveQuantity 불량(DEFECTIVE) 재고의 보유 수량 합계
 */
public record InventorySkuSummary(
        Long skuId,
        String skuCode,
        String skuName,
        String unit,
        Long totalQuantity,
        Long availableQuantity,
        Long allocatedQuantity,
        Long defectiveQuantity
) {
}

package com.kb.wms.inventory.application.port.in.result;

/**
 * 안전 재고 미만 SKU (GET /api/v1/inventory/low-stock).
 *
 * @param shortageQuantity safetyStockQuantity - availableQuantity
 */
public record LowStockItem(
        Long skuId,
        String skuCode,
        String skuName,
        String unit,
        Long safetyStockQuantity,
        Long availableQuantity,
        Long shortageQuantity
) {

    /**
     * 조회 쿼리(JPQL 생성자 표현식)용. 부족 수량은 여기서 계산한다.
     */
    public LowStockItem(Long skuId, String skuCode, String skuName, String unit,
                        Long safetyStockQuantity, Long availableQuantity) {
        this(skuId, skuCode, skuName, unit, safetyStockQuantity, availableQuantity,
                safetyStockQuantity - availableQuantity);
    }
}

package com.kb.wms.product.application.port.in.command;

import java.math.BigDecimal;

/**
 * PATCH /api/v1/products/skus/{skuId} 요청. 모든 필드는 선택이며, 값이 있는 필드만 부분 수정한다.
 * skuCode, productId, unit, 상태는 이 커맨드로 바꾸지 않는다.
 */
public record ProductSkuUpdateCommand(
        Long skuId,
        String skuName,
        String barcode,
        BigDecimal weight,
        BigDecimal currentPurchasePrice,
        BigDecimal currentSupplyPrice,
        Long safetyStockQuantity
) {

    public boolean hasNoChanges() {
        return skuName == null && barcode == null && weight == null && currentPurchasePrice == null
                && currentSupplyPrice == null && safetyStockQuantity == null;
    }
}

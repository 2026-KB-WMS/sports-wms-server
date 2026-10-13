package com.kb.wms.product.application.port.in.command;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 코드·이름은 상품 코드·이름과 옵션 값으로 서버가 항상 만든다(ADR-026).
 */
public record ProductSkuRegisterCommand(
        Long productId,
        String barcode,
        BigDecimal weight,
        BigDecimal currentPurchasePrice,
        BigDecimal currentSupplyPrice,
        String unit,
        Long safetyStockQuantity,
        List<Long> optionValueIds
) {
}

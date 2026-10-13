package com.kb.wms.product.application.port.in.command;

import java.math.BigDecimal;
import java.util.List;

/**
 * skuCode·name이 null이면 상품 코드·이름과 옵션 값으로 서버가 만든다(ADR-025).
 */
public record ProductSkuRegisterCommand(
        Long productId,
        String skuCode,
        String barcode,
        String name,
        BigDecimal weight,
        BigDecimal currentPurchasePrice,
        BigDecimal currentSupplyPrice,
        String unit,
        Long safetyStockQuantity,
        List<Long> optionValueIds
) {
}

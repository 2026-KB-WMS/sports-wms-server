package com.kb.wms.product.application.port.in.command;

import java.math.BigDecimal;

public record ProductSkuRegisterCommand(
        Long productId,
        String skuCode,
        String barcode,
        String name,
        BigDecimal weight,
        BigDecimal currentPurchasePrice,
        BigDecimal currentSupplyPrice,
        String unit,
        BigDecimal safetyStockQuantity
) {
}

package com.kb.wms.product.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.product.domain.entity.ProductSku;

/**
 * PATCH /api/v1/products/skus/{skuId} 응답. 수정은 본사만 호출하므로 매입 단가를 그대로 담는다.
 */
public record ProductSkuUpdateResponse(
        Long skuId,
        Long productId,
        String skuCode,
        String barcode,
        String skuName,
        BigDecimal weight,
        BigDecimal currentPurchasePrice,
        BigDecimal currentSupplyPrice,
        String unit,
        Long safetyStockQuantity,
        boolean isActive,
        LocalDateTime updatedAt
) {

    public static ProductSkuUpdateResponse from(ProductSku sku) {
        return new ProductSkuUpdateResponse(
                sku.getSkuId(),
                sku.getProductId(),
                sku.getSkuCode(),
                sku.getBarcode(),
                sku.getName(),
                sku.getWeight(),
                sku.getCurrentPurchasePrice(),
                sku.getCurrentSupplyPrice(),
                sku.getUnit(),
                sku.getSafetyStockQuantity(),
                sku.isActive(),
                sku.getUpdatedAt());
    }
}

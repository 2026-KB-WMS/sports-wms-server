package com.kb.wms.product.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.product.domain.entity.ProductSku;

/**
 * POST /api/v1/products/skus 응답. 등록 직후에는 옵션이 연결되지 않은 상태이므로 optionValues는 항상 빈 배열이다.
 */
public record ProductSkuCreateResponse(
        Long skuId,
        Long productId,
        String skuCode,
        String barcode,
        String skuName,
        BigDecimal weight,
        BigDecimal currentPurchasePrice,
        BigDecimal currentSupplyPrice,
        String unit,
        BigDecimal safetyStockQuantity,
        boolean isActive,
        List<SkuOptionValueResponse> optionValues,
        LocalDateTime createdAt
) {

    public static ProductSkuCreateResponse from(ProductSku sku) {
        return new ProductSkuCreateResponse(
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
                List.of(),
                sku.getCreatedAt());
    }
}

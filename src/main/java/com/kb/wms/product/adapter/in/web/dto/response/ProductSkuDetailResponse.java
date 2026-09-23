package com.kb.wms.product.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.product.application.port.in.result.SkuOptionSummary;
import com.kb.wms.product.domain.entity.ProductSku;

/**
 * GET /api/v1/products/skus/{skuId} 응답.
 */
public record ProductSkuDetailResponse(
        Long skuId,
        String skuCode,
        String barcode,
        String skuName,
        Long productId,
        String productCode,
        String productName,
        Long brandId,
        String brandName,
        Long categoryId,
        String categoryName,
        String unit,
        BigDecimal weight,
        BigDecimal currentPurchasePrice,
        BigDecimal currentSupplyPrice,
        Long safetyStockQuantity,
        boolean isActive,
        List<SkuOptionValueResponse> optionValues,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductSkuDetailResponse of(
            ProductSku sku, String productCode, String productName,
            Long brandId, String brandName, Long categoryId, String categoryName,
            List<SkuOptionSummary> options) {
        return new ProductSkuDetailResponse(
                sku.getSkuId(),
                sku.getSkuCode(),
                sku.getBarcode(),
                sku.getName(),
                sku.getProductId(),
                productCode,
                productName,
                brandId,
                brandName,
                categoryId,
                categoryName,
                sku.getUnit(),
                sku.getWeight(),
                sku.getCurrentPurchasePrice(),
                sku.getCurrentSupplyPrice(),
                sku.getSafetyStockQuantity(),
                sku.isActive(),
                SkuOptionValueResponse.listFrom(options),
                sku.getCreatedAt(),
                sku.getUpdatedAt());
    }
}

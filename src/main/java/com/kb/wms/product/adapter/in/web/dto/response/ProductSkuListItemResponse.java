package com.kb.wms.product.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.kb.wms.product.application.port.in.result.SkuOptionSummary;
import com.kb.wms.product.domain.entity.ProductSku;

/**
 * GET /api/v1/products/skus 목록 응답 항목.
 */
public record ProductSkuListItemResponse(
        Long skuId,
        String skuCode,
        String barcode,
        String skuName,
        Long productId,
        String productName,
        String unit,
        BigDecimal weight,
        BigDecimal currentPurchasePrice,
        BigDecimal currentSupplyPrice,
        Long safetyStockQuantity,
        boolean isActive,
        List<SkuOptionValueResponse> optionValues
) {

    public static ProductSkuListItemResponse of(ProductSku sku, String productName, List<SkuOptionSummary> options) {
        return new ProductSkuListItemResponse(
                sku.getSkuId(),
                sku.getSkuCode(),
                sku.getBarcode(),
                sku.getName(),
                sku.getProductId(),
                productName,
                sku.getUnit(),
                sku.getWeight(),
                sku.getCurrentPurchasePrice(),
                sku.getCurrentSupplyPrice(),
                sku.getSafetyStockQuantity(),
                sku.isActive(),
                SkuOptionValueResponse.listFrom(options));
    }
}

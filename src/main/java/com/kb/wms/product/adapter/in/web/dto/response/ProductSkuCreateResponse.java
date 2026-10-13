package com.kb.wms.product.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.product.application.port.in.result.SkuOptionSummary;
import com.kb.wms.product.domain.entity.ProductSku;

/**
 * POST /api/v1/products/skus 응답. optionValues는 등록할 때 함께 연결한 옵션 값이다.
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
        Long safetyStockQuantity,
        boolean isActive,
        List<SkuOptionValueResponse> optionValues,
        LocalDateTime createdAt
) {

    public static ProductSkuCreateResponse of(ProductSku sku, List<SkuOptionSummary> options) {
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
                SkuOptionValueResponse.listFrom(options),
                sku.getCreatedAt());
    }
}

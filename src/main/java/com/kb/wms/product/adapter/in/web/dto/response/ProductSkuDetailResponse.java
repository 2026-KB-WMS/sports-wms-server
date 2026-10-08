package com.kb.wms.product.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kb.wms.product.application.port.in.result.SkuOptionSummary;
import com.kb.wms.product.domain.entity.ProductSku;

/**
 * GET /api/v1/products/skus/{skuId} 응답.
 * 매입가·안전재고는 점주(STORE_OWNER)에게 노출하지 않으므로 값이 없으면 필드를 생략한다(미설정 값도 마찬가지).
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
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal currentPurchasePrice,
        BigDecimal currentSupplyPrice,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long safetyStockQuantity,
        boolean isActive,
        List<SkuOptionValueResponse> optionValues,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** @param includePurchaseData 매입가·안전재고를 담을지 여부. 점주에게는 false */
    public static ProductSkuDetailResponse of(
            ProductSku sku, String productCode, String productName,
            Long brandId, String brandName, Long categoryId, String categoryName,
            List<SkuOptionSummary> options, boolean includePurchaseData) {
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
                includePurchaseData ? sku.getCurrentPurchasePrice() : null,
                sku.getCurrentSupplyPrice(),
                includePurchaseData ? sku.getSafetyStockQuantity() : null,
                sku.isActive(),
                SkuOptionValueResponse.listFrom(options),
                sku.getCreatedAt(),
                sku.getUpdatedAt());
    }
}

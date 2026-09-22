package com.kb.wms.product.adapter.in.web.dto.response;

import com.kb.wms.product.domain.entity.Product;

/**
 * GET /api/v1/products 목록 응답 항목.
 */
public record ProductSummaryResponse(
        Long productId,
        String productCode,
        String productName,
        Long brandId,
        String brandName,
        Long categoryId,
        String categoryName,
        boolean isActive
) {

    public static ProductSummaryResponse of(Product product, String brandName, String categoryName) {
        return new ProductSummaryResponse(
                product.getProductId(),
                product.getProductCode(),
                product.getName(),
                product.getBrandId(),
                brandName,
                product.getCategoryId(),
                categoryName,
                product.isActive());
    }
}

package com.kb.wms.product.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.product.domain.entity.Product;

/**
 * POST /api/v1/products, GET /api/v1/products/{productId} 응답.
 */
public record ProductDetailResponse(
        Long productId,
        String productCode,
        String productName,
        String description,
        Long brandId,
        String brandName,
        Long categoryId,
        String categoryName,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductDetailResponse of(Product product, String brandName, String categoryName) {
        return new ProductDetailResponse(
                product.getProductId(),
                product.getProductCode(),
                product.getName(),
                product.getDescription(),
                product.getBrandId(),
                brandName,
                product.getCategoryId(),
                categoryName,
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}

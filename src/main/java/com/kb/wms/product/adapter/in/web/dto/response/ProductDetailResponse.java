package com.kb.wms.product.adapter.in.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.product.application.port.in.result.ProductSpecSummary;
import com.kb.wms.product.domain.entity.Product;

/**
 * POST /api/v1/products, GET /api/v1/products/{productId}, PATCH /api/v1/products/{productId} 응답.
 * specs는 상품에 지정된 스펙 값이며, 없으면 빈 배열이다.
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
        List<ProductSpecResponse> specs,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductDetailResponse of(Product product, String brandName, String categoryName,
                                           List<ProductSpecSummary> specs) {
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
                ProductSpecResponse.listFrom(specs),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}

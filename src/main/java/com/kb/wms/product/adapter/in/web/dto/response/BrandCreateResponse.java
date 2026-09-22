package com.kb.wms.product.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.product.domain.entity.Brand;

/**
 * POST /api/v1/products/brands 응답.
 */
public record BrandCreateResponse(
        Long brandId,
        String brandName,
        String description,
        boolean isActive,
        LocalDateTime createdAt
) {

    public static BrandCreateResponse from(Brand brand) {
        return new BrandCreateResponse(
                brand.getBrandId(),
                brand.getName(),
                brand.getDescription(),
                brand.isActive(),
                brand.getCreatedAt());
    }
}

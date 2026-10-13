package com.kb.wms.product.adapter.in.web.dto.response;

import com.kb.wms.product.domain.entity.Brand;

/**
 * GET /api/v1/products/brands 응답 항목.
 */
public record BrandResponse(
        Long brandId,
        String brandCode,
        String brandName,
        String description,
        boolean isActive
) {

    public static BrandResponse from(Brand brand) {
        return new BrandResponse(brand.getBrandId(), brand.getBrandCode(), brand.getName(), brand.getDescription(),
                brand.isActive());
    }
}

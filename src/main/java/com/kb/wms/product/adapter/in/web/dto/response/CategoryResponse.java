package com.kb.wms.product.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.product.domain.entity.Category;

/**
 * POST/GET /api/v1/products/categories 응답.
 */
public record CategoryResponse(
        Long categoryId,
        Long parentCategoryId,
        String categoryCode,
        String categoryName,
        int depth,
        int sortOrder,
        boolean isActive,
        LocalDateTime createdAt
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getParentCategoryId(),
                category.getCategoryCode(),
                category.getName(),
                category.getDepth(),
                category.getSortOrder(),
                category.isActive(),
                category.getCreatedAt());
    }
}

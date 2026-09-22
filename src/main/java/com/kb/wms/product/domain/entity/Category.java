package com.kb.wms.product.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.product.domain.enums.ProductStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품을 분류하는 계층형 카테고리 마스터.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class    Category {

    private Long categoryId;
    private Long parentCategoryId;
    private String categoryCode;
    private String name;
    private int depth;
    private int sortOrder;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Category(Long categoryId, Long parentCategoryId, String categoryCode, String name,
                      int depth, int sortOrder, ProductStatus status,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.categoryId = categoryId;
        this.parentCategoryId = parentCategoryId;
        this.categoryCode = categoryCode;
        this.name = name;
        this.depth = depth;
        this.sortOrder = sortOrder;
        this.status = status == null ? ProductStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isRoot() {
        return this.parentCategoryId == null;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == ProductStatus.ACTIVE;
    }
}

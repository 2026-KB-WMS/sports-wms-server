package com.kb.wms.product.adapter.out.persistence.entity;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.enums.ProductStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "parent_category_id")
    private Long parentCategoryId;

    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Builder
    private CategoryJpaEntity(Long categoryId, Long parentCategoryId, String categoryCode, String name,
                               int depth, int sortOrder, ProductStatus status) {
        this.categoryId = categoryId;
        this.parentCategoryId = parentCategoryId;
        this.categoryCode = categoryCode;
        this.name = name;
        this.depth = depth;
        this.sortOrder = sortOrder;
        this.status = status;
    }

    public static CategoryJpaEntity fromDomain(Category category) {
        return CategoryJpaEntity.builder()
                .categoryId(category.getCategoryId())
                .parentCategoryId(category.getParentCategoryId())
                .categoryCode(category.getCategoryCode())
                .name(category.getName())
                .depth(category.getDepth())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .build();
    }

    public Category toDomain() {
        return Category.builder()
                .categoryId(categoryId)
                .parentCategoryId(parentCategoryId)
                .categoryCode(categoryCode)
                .name(name)
                .depth(depth)
                .sortOrder(sortOrder)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

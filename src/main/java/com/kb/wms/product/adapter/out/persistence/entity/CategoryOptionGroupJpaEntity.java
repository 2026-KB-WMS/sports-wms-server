package com.kb.wms.product.adapter.out.persistence.entity;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.product.domain.entity.CategoryOptionGroup;
import com.kb.wms.product.domain.enums.OptionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category_option_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_category_option_group",
                columnNames = {"category_id", "option_group_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryOptionGroupJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_option_group_id")
    private Long categoryOptionGroupId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "option_group_id", nullable = false)
    private Long optionGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, length = 10)
    private OptionType optionType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    private CategoryOptionGroupJpaEntity(Long categoryOptionGroupId, Long categoryId, Long optionGroupId,
                                          OptionType optionType, int sortOrder) {
        this.categoryOptionGroupId = categoryOptionGroupId;
        this.categoryId = categoryId;
        this.optionGroupId = optionGroupId;
        this.optionType = optionType;
        this.sortOrder = sortOrder;
    }

    public static CategoryOptionGroupJpaEntity fromDomain(CategoryOptionGroup domain) {
        return CategoryOptionGroupJpaEntity.builder()
                .categoryOptionGroupId(domain.getCategoryOptionGroupId())
                .categoryId(domain.getCategoryId())
                .optionGroupId(domain.getOptionGroupId())
                .optionType(domain.getOptionType())
                .sortOrder(domain.getSortOrder())
                .build();
    }

    public CategoryOptionGroup toDomain() {
        return CategoryOptionGroup.builder()
                .categoryOptionGroupId(categoryOptionGroupId)
                .categoryId(categoryId)
                .optionGroupId(optionGroupId)
                .optionType(optionType)
                .sortOrder(sortOrder)
                .createdAt(getCreatedAt())
                .build();
    }
}

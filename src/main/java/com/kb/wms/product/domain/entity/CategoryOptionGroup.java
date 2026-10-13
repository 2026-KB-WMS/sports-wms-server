package com.kb.wms.product.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.product.domain.enums.OptionType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카테고리에 지정한 옵션 그룹과 용도(SPEC·SKU). 하위 카테고리는 상위의 지정을 상속한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryOptionGroup {

    private Long categoryOptionGroupId;
    private Long categoryId;
    private Long optionGroupId;
    private OptionType optionType;
    private int sortOrder;
    private LocalDateTime createdAt;

    @Builder
    private CategoryOptionGroup(Long categoryOptionGroupId, Long categoryId, Long optionGroupId,
                                 OptionType optionType, int sortOrder, LocalDateTime createdAt) {
        this.categoryOptionGroupId = categoryOptionGroupId;
        this.categoryId = categoryId;
        this.optionGroupId = optionGroupId;
        this.optionType = optionType;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }

    public static CategoryOptionGroup assign(Long categoryId, Long optionGroupId, OptionType optionType,
                                              int sortOrder) {
        return CategoryOptionGroup.builder()
                .categoryId(categoryId)
                .optionGroupId(optionGroupId)
                .optionType(optionType)
                .sortOrder(sortOrder)
                .build();
    }
}

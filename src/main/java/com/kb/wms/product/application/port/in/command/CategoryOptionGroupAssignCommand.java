package com.kb.wms.product.application.port.in.command;

import java.util.List;

import com.kb.wms.product.domain.enums.OptionType;

/**
 * PUT /api/v1/products/categories/{categoryId}/option-groups 요청. 카테고리의 지정 전체를 바꾼다.
 */
public record CategoryOptionGroupAssignCommand(
        Long categoryId,
        List<Item> items
) {

    public record Item(
            Long optionGroupId,
            OptionType optionType,
            int sortOrder
    ) {
    }
}

package com.kb.wms.product.adapter.in.web.dto.response;

import java.util.List;

import com.kb.wms.product.application.port.in.result.CategoryOptionGroupSummary;
import com.kb.wms.product.domain.enums.OptionType;

/**
 * GET·PUT /api/v1/products/categories/{categoryId}/option-groups 응답 항목.
 * inherited는 상위 카테고리에서 상속한 지정이면 true이고, definedCategoryId는 지정을 등록한 카테고리다.
 */
public record CategoryOptionGroupResponse(
        Long optionGroupId,
        String name,
        OptionType optionType,
        int sortOrder,
        Long definedCategoryId,
        boolean inherited,
        List<OptionValueItemResponse> values
) {

    public static CategoryOptionGroupResponse of(CategoryOptionGroupSummary summary, Long categoryId) {
        return new CategoryOptionGroupResponse(
                summary.optionGroupId(),
                summary.name(),
                summary.optionType(),
                summary.sortOrder(),
                summary.definedCategoryId(),
                !summary.definedCategoryId().equals(categoryId),
                summary.values().stream().map(OptionValueItemResponse::from).toList());
    }
}

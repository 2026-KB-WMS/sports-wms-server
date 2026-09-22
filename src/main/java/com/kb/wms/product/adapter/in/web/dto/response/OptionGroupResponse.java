package com.kb.wms.product.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.product.domain.entity.OptionGroup;

/**
 * POST /api/v1/products/option-groups 응답.
 */
public record OptionGroupResponse(
        Long optionGroupId,
        String name,
        LocalDateTime createdAt
) {

    public static OptionGroupResponse from(OptionGroup optionGroup) {
        return new OptionGroupResponse(optionGroup.getOptionGroupId(), optionGroup.getName(), optionGroup.getCreatedAt());
    }
}

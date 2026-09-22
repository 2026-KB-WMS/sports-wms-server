package com.kb.wms.product.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.product.domain.entity.OptionValue;

/**
 * POST /api/v1/products/option-groups/{optionGroupId}/values 응답.
 */
public record OptionValueResponse(
        Long optionValueId,
        Long optionGroupId,
        String value,
        int sortOrder,
        boolean isActive,
        LocalDateTime createdAt
) {

    public static OptionValueResponse from(OptionValue optionValue) {
        return new OptionValueResponse(
                optionValue.getOptionValueId(),
                optionValue.getOptionGroupId(),
                optionValue.getValue(),
                optionValue.getSortOrder(),
                optionValue.isActive(),
                optionValue.getCreatedAt());
    }
}

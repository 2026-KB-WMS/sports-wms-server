package com.kb.wms.product.adapter.in.web.dto.request;

import com.kb.wms.product.application.port.in.command.OptionValueRegisterCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/products/option-groups/{optionGroupId}/values 요청 바디.
 */
public record OptionValueRegisterRequest(
        @NotBlank(message = "옵션 값은 필수 값입니다.")
        @Size(max = 100, message = "옵션 값은 최대 100자입니다.")
        String value,

        @PositiveOrZero(message = "노출 순서는 0 이상이어야 합니다.")
        Integer sortOrder
) {

    public OptionValueRegisterCommand toCommand(Long optionGroupId) {
        return new OptionValueRegisterCommand(optionGroupId, value, sortOrder == null ? 0 : sortOrder);
    }
}

package com.kb.wms.product.adapter.in.web.dto.request;

import com.kb.wms.product.application.port.in.command.CategoryRegisterCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/products/categories 요청 바디.
 */
public record CategoryRegisterRequest(
        Long parentCategoryId,

        @NotBlank(message = "카테고리 코드는 필수 값입니다.")
        @Size(max = 50, message = "카테고리 코드는 최대 50자입니다.")
        String categoryCode,

        @NotBlank(message = "카테고리명은 필수 값입니다.")
        @Size(max = 100, message = "카테고리명은 최대 100자입니다.")
        String categoryName,

        @PositiveOrZero(message = "노출 순서는 0 이상이어야 합니다.")
        Integer sortOrder
) {

    public CategoryRegisterCommand toCommand() {
        return new CategoryRegisterCommand(
                parentCategoryId, categoryCode, categoryName, sortOrder == null ? 0 : sortOrder);
    }
}

package com.kb.wms.product.adapter.in.web.dto.request;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.command.CategoryUpdateCommand;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/products/categories/{categoryId} 요청 바디. 모든 필드는 선택이며, 값이 있는 필드만 수정한다.
 * categoryCode와 parentCategoryId는 불변이므로 검증 애노테이션 대신 toCommand()에서 명시적으로 거부해,
 * 명세대로 400 VALIDATION_ERROR로 응답한다.
 */
public record CategoryUpdateRequest(
        @Size(max = 100, message = "카테고리명은 최대 100자입니다.")
        String categoryName,

        @PositiveOrZero(message = "노출 순서는 0 이상이어야 합니다.")
        Integer sortOrder,

        String categoryCode,

        Long parentCategoryId
) {

    public CategoryUpdateCommand toCommand(Long categoryId) {
        if (categoryCode != null || parentCategoryId != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "categoryCode와 parentCategoryId는 이 API로 수정할 수 없습니다.");
        }
        if (categoryName != null && categoryName.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "카테고리명은 비워 둘 수 없습니다.");
        }
        return new CategoryUpdateCommand(categoryId, categoryName, sortOrder);
    }
}

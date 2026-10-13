package com.kb.wms.product.adapter.in.web.dto.request;

import java.util.List;

import com.kb.wms.product.application.port.in.command.CategoryOptionGroupAssignCommand;
import com.kb.wms.product.domain.enums.OptionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * PUT /api/v1/products/categories/{categoryId}/option-groups 요청 바디. 빈 목록이면 카테고리 자신의 지정을 모두 지운다.
 */
public record CategoryOptionGroupAssignRequest(
        @NotNull(message = "items는 필수 값입니다.")
        List<@Valid @NotNull(message = "items 항목은 비어 있을 수 없습니다.") Item> items
) {

    public CategoryOptionGroupAssignCommand toCommand(Long categoryId) {
        return new CategoryOptionGroupAssignCommand(categoryId, items.stream()
                .map(item -> new CategoryOptionGroupAssignCommand.Item(
                        item.optionGroupId(), item.optionType(), item.sortOrder() == null ? 0 : item.sortOrder()))
                .toList());
    }

    public record Item(
            @NotNull(message = "옵션 그룹 ID는 필수 값입니다.")
            Long optionGroupId,

            @NotNull(message = "옵션 용도(SPEC, SKU)는 필수 값입니다.")
            OptionType optionType,

            @PositiveOrZero(message = "노출 순서는 0 이상이어야 합니다.")
            Integer sortOrder
    ) {
    }
}

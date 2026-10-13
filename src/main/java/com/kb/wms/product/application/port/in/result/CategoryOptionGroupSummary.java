package com.kb.wms.product.application.port.in.result;

import java.util.List;

import com.kb.wms.product.domain.enums.OptionType;

/**
 * 카테고리에 적용되는 옵션 그룹 하나(상위 카테고리에서 상속한 것 포함)와 선택 가능한 옵션 값.
 * definedCategoryId는 이 지정을 실제로 등록한 카테고리다.
 */
public record CategoryOptionGroupSummary(
        Long optionGroupId,
        String name,
        OptionType optionType,
        int sortOrder,
        Long definedCategoryId,
        List<OptionValueSummary> values
) {
}

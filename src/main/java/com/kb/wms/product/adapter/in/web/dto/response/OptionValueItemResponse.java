package com.kb.wms.product.adapter.in.web.dto.response;

import com.kb.wms.product.application.port.in.result.OptionValueSummary;

/**
 * 옵션 그룹 조회 응답들(상품별·전체·카테고리별)의 values 항목. optionCode는 코드 도입 전 값이면 null이다.
 */
public record OptionValueItemResponse(
        Long optionValueId,
        String optionCode,
        String value,
        int sortOrder,
        boolean isActive
) {

    public static OptionValueItemResponse from(OptionValueSummary summary) {
        return new OptionValueItemResponse(summary.optionValueId(), summary.optionCode(), summary.value(),
                summary.sortOrder(), summary.isActive());
    }
}

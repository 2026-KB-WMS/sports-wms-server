package com.kb.wms.product.application.port.in.result;

/**
 * 옵션 그룹에 속한 옵션 값 하나를 간단히 표현한 조회 결과.
 * GET /products/{productId}/option-groups 응답의 values 항목에 대응한다.
 */
public record OptionValueSummary(
        Long optionValueId,
        String value,
        int sortOrder
) {
}

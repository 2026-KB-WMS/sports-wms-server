package com.kb.wms.product.application.port.in.result;

/**
 * 옵션 그룹에 속한 옵션 값 하나를 간단히 표현한 조회 결과.
 * GET /products/option-groups, GET /products/{productId}/option-groups,
 * GET /products/categories/{categoryId}/option-groups 응답의 values 항목에 대응한다.
 * optionCode는 코드 도입 전에 만든 값이면 null이다.
 */
public record OptionValueSummary(
        Long optionValueId,
        String optionCode,
        String value,
        int sortOrder,
        boolean isActive
) {
}

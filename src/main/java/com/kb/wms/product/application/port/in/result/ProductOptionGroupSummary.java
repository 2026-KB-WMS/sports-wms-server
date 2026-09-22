package com.kb.wms.product.application.port.in.result;

import java.util.List;

/**
 * 특정 상품의 SKU들에 실제로 연결된 옵션 그룹 하나와, 그 안에서 사용 중인 옵션 값 목록.
 * GET /products/{productId}/option-groups 응답의 items 항목에 대응한다.
 */
public record ProductOptionGroupSummary(
        Long optionGroupId,
        String name,
        List<OptionValueSummary> values
) {
}

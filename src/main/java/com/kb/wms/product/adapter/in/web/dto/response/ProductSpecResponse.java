package com.kb.wms.product.adapter.in.web.dto.response;

import java.util.List;

import com.kb.wms.product.application.port.in.result.ProductSpecSummary;

/**
 * 상품에 지정된 스펙 값 하나. 상품 등록·상세 응답의 specs 항목이다.
 */
public record ProductSpecResponse(
        Long optionGroupId,
        String optionGroupName,
        Long optionValueId,
        String optionCode,
        String value
) {

    public static List<ProductSpecResponse> listFrom(List<ProductSpecSummary> summaries) {
        return summaries.stream()
                .map(summary -> new ProductSpecResponse(summary.optionGroupId(), summary.optionGroupName(),
                        summary.optionValueId(), summary.optionCode(), summary.value()))
                .toList();
    }
}

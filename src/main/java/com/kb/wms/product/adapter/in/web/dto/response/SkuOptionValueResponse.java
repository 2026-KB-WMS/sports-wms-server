package com.kb.wms.product.adapter.in.web.dto.response;

import java.util.List;

import com.kb.wms.product.application.port.in.result.SkuOptionSummary;

/**
 * SKU에 연결된 옵션 값 하나. GET/POST SKU 관련 응답의 optionValues 항목에 공통으로 쓰인다.
 */
public record SkuOptionValueResponse(
        Long optionGroupId,
        String optionGroupName,
        Long optionValueId,
        String value
) {

    public static SkuOptionValueResponse from(SkuOptionSummary summary) {
        return new SkuOptionValueResponse(
                summary.optionGroupId(), summary.optionGroupName(), summary.optionValueId(), summary.value());
    }

    public static List<SkuOptionValueResponse> listFrom(List<SkuOptionSummary> summaries) {
        return summaries.stream().map(SkuOptionValueResponse::from).toList();
    }
}

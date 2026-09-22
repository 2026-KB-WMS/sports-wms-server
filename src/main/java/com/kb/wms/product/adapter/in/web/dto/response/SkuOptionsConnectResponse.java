package com.kb.wms.product.adapter.in.web.dto.response;

import java.util.List;

import com.kb.wms.product.application.port.in.result.SkuOptionSummary;

/**
 * POST /api/v1/products/skus/{skuId}/options 응답.
 */
public record SkuOptionsConnectResponse(
        Long skuId,
        List<SkuOptionValueResponse> optionValues
) {

    public static SkuOptionsConnectResponse of(Long skuId, List<SkuOptionSummary> options) {
        return new SkuOptionsConnectResponse(skuId, SkuOptionValueResponse.listFrom(options));
    }
}

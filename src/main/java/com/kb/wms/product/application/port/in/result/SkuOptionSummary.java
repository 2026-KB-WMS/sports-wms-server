package com.kb.wms.product.application.port.in.result;

/**
 * SKU에 연결된 옵션 값을, 웹 응답에 바로 쓸 수 있도록 옵션 그룹명까지 조인해 담은 조회 결과.
 * GET /products/skus, GET /products/skus/{skuId}, POST /products/skus/{skuId}/options 응답의
 * optionValues 항목에 대응한다.
 */
public record SkuOptionSummary(
        Long optionGroupId,
        String optionGroupName,
        Long optionValueId,
        String value
) {
}

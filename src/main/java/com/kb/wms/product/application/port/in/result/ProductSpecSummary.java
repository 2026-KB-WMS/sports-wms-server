package com.kb.wms.product.application.port.in.result;

/**
 * 상품에 지정된 스펙 값 하나를 옵션 그룹명까지 조인해 담은 조회 결과.
 * 상품 등록·상세 응답의 specs 항목에 대응한다.
 */
public record ProductSpecSummary(
        Long optionGroupId,
        String optionGroupName,
        Long optionValueId,
        String optionCode,
        String value
) {
}

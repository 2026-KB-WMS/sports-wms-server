package com.kb.wms.product.domain.enums;

/**
 * 카테고리에 지정하는 옵션 그룹의 용도(ADR-025).
 * SPEC은 상품 하나에 값이 정해지는 스펙, SKU는 SKU마다 달라지는 옵션이다.
 */
public enum OptionType {

    SPEC,
    SKU
}

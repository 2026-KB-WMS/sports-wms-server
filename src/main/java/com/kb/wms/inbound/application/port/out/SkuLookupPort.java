package com.kb.wms.inbound.application.port.out;

/**
 * 상품 도메인의 SKU 존재 여부를 입고 도메인에서 확인하기 위한 아웃바운드 포트.
 */
public interface SkuLookupPort {

    /** SKU가 없으면 상품 도메인의 404 SKU_NOT_FOUND 예외를 던진다. */
    void requireExists(Long skuId);
}

package com.kb.wms.inventory.application.port.out;

/**
 * 상품 도메인의 SKU 상태를 재고 도메인에서 확인하기 위한 아웃바운드 포트.
 */
public interface SkuStatusPort {

    /** SKU가 비활성이면 SKU_NOT_ACTIVE 예외를 던진다. 없는 SKU는 상품 도메인의 SKU_NOT_FOUND. */
    void requireActive(Long skuId);
}

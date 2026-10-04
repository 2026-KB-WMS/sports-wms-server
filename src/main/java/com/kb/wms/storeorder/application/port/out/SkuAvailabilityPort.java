package com.kb.wms.storeorder.application.port.out;

/**
 * 상품 도메인의 SKU 활성 여부를 지점 발주 도메인에서 확인하기 위한 아웃바운드 포트.
 * 발주 승인 때 항목 SKU가 여전히 발주 가능한지 검증하는 데 쓴다.
 */
public interface SkuAvailabilityPort {

    /**
     * SKU가 없으면 상품 도메인의 SKU_NOT_FOUND, SKU 또는 상품이 비활성이면 409 CONFLICT 예외를 던진다.
     * 상품을 비활성화하면 하위 SKU도 같은 트랜잭션에서 비활성화되므로 SKU 상태만 확인하면 된다.
     */
    void requireActive(Long skuId);
}

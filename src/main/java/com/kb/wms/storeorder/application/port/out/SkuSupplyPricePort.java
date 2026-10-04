package com.kb.wms.storeorder.application.port.out;

import java.math.BigDecimal;

/**
 * 상품 도메인의 SKU 공급 단가를 지점 발주 도메인에서 가져오기 위한 아웃바운드 포트.
 */
public interface SkuSupplyPricePort {

    /**
     * 발주 등록 시점의 공급 단가(current_supply_price)를 반환한다.
     * SKU가 없으면 상품 도메인의 SKU_NOT_FOUND, SKU 또는 상품이 비활성이면 409 CONFLICT,
     * 공급 단가가 없으면 409 SUPPLY_PRICE_MISSING 예외를 던진다.
     * 상품을 비활성화하면 하위 SKU도 같은 트랜잭션에서 비활성화되므로 SKU 상태만 확인하면 된다.
     */
    BigDecimal getOrderableSupplyPrice(Long skuId);
}

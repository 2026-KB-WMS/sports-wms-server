package com.kb.wms.inbound.application.port.out;

import java.math.BigDecimal;

/**
 * 상품 도메인의 SKU 매입 단가를 입고 도메인에서 가져오기 위한 아웃바운드 포트.
 */
public interface SkuPurchasePricePort {

    /**
     * 발주 등록 시점의 매입 단가(current_purchase_price)를 반환한다.
     * SKU가 없으면 상품 도메인의 SKU_NOT_FOUND, 비활성이면 409 CONFLICT,
     * 매입 단가가 없으면 409 PURCHASE_PRICE_MISSING 예외를 던진다.
     */
    BigDecimal getPurchasablePrice(Long skuId);
}

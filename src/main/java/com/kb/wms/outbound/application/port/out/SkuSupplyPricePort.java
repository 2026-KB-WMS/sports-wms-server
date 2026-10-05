package com.kb.wms.outbound.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 피킹 완료 때 발주 항목에 공급 단가 스냅샷이 없을 경우 쓰는 SKU 현재 공급 단가 조회 포트(출고 → 상품 방향).
 */
public interface SkuSupplyPricePort {

    /** SKU의 현재 공급 단가. 단가가 비어 있으면 빈 값이다. */
    Optional<BigDecimal> findCurrentSupplyPrice(Long skuId);
}

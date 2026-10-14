package com.kb.wms.product.application.port.in.result;

import java.math.BigDecimal;

/**
 * 다른 도메인이 SKU의 활성 여부와 현재 단가를 확인할 때 쓰는 읽기용 값. 엔티티를 밖으로 내보내지 않는다.
 */
public record ProductSkuView(
        Long skuId,
        boolean active,
        BigDecimal currentPurchasePrice,
        BigDecimal currentSupplyPrice
) {
}

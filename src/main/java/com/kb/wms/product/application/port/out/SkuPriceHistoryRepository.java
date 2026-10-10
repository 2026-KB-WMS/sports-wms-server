package com.kb.wms.product.application.port.out;

import com.kb.wms.product.domain.entity.SkuPriceHistory;

/**
 * SKU 단가 변경 이력 영속성 아웃바운드 포트.
 */
public interface SkuPriceHistoryRepository {

    SkuPriceHistory save(SkuPriceHistory history);
}

package com.kb.wms.product.application.port.out;

import java.util.List;

import com.kb.wms.product.domain.entity.SkuOptionValue;

/**
 * SKU-옵션 값 연결 영속성 아웃바운드 포트.
 */
public interface SkuOptionValueRepository {

    SkuOptionValue save(SkuOptionValue skuOptionValue);

    List<SkuOptionValue> findBySkuId(Long skuId);

    /** 여러 SKU의 옵션 연결을 쿼리 한 번으로 조회한다(목록 응답 N+1 방지). */
    List<SkuOptionValue> findBySkuIdIn(java.util.Collection<Long> skuIds);

    boolean existsBySkuIdAndOptionValueId(Long skuId, Long optionValueId);
}

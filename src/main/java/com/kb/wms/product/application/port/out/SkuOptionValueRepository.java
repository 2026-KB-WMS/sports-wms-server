package com.kb.wms.product.application.port.out;

import java.util.List;

import com.kb.wms.product.domain.entity.SkuOptionValue;

/**
 * SKU-옵션 값 연결 영속성 아웃바운드 포트.
 */
public interface SkuOptionValueRepository {

    SkuOptionValue save(SkuOptionValue skuOptionValue);

    List<SkuOptionValue> findBySkuId(Long skuId);

    boolean existsBySkuIdAndOptionValueId(Long skuId, Long optionValueId);
}

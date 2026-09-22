package com.kb.wms.product.domain.entity;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SKU와 옵션 값을 연결해 SKU 구성을 확정하는 매핑.
 * 식별자는 (skuId, optionValueId) 복합키다.
 */
@Getter
@EqualsAndHashCode(of = {"skuId", "optionValueId"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkuOptionValue {

    private Long skuId;
    private Long optionValueId;
    private LocalDateTime createdAt;

    @Builder
    private SkuOptionValue(Long skuId, Long optionValueId, LocalDateTime createdAt) {
        this.skuId = skuId;
        this.optionValueId = optionValueId;
        this.createdAt = createdAt;
    }

    public static SkuOptionValue connect(Long skuId, Long optionValueId) {
        return SkuOptionValue.builder()
                .skuId(skuId)
                .optionValueId(optionValueId)
                .build();
    }
}

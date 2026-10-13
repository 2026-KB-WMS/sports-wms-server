package com.kb.wms.product.domain.entity;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품에 지정한 스펙 값. 상품당 옵션 그룹 하나에 값 하나만 둘 수 있다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSpec {

    private Long productSpecId;
    private Long productId;
    private Long optionValueId;
    private LocalDateTime createdAt;

    @Builder
    private ProductSpec(Long productSpecId, Long productId, Long optionValueId, LocalDateTime createdAt) {
        this.productSpecId = productSpecId;
        this.productId = productId;
        this.optionValueId = optionValueId;
        this.createdAt = createdAt;
    }

    public static ProductSpec assign(Long productId, Long optionValueId) {
        return ProductSpec.builder()
                .productId(productId)
                .optionValueId(optionValueId)
                .build();
    }
}

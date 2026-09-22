package com.kb.wms.product.adapter.out.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * sku_option_value의 (sku_id, option_value_id) 복합키.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkuOptionValueId implements Serializable {

    @Column(name = "sku_id")
    private Long skuId;

    @Column(name = "option_value_id")
    private Long optionValueId;

    public SkuOptionValueId(Long skuId, Long optionValueId) {
        this.skuId = skuId;
        this.optionValueId = optionValueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SkuOptionValueId that)) {
            return false;
        }
        return Objects.equals(skuId, that.skuId) && Objects.equals(optionValueId, that.optionValueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skuId, optionValueId);
    }
}

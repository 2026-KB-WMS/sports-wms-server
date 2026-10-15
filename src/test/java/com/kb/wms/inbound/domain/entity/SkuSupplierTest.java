package com.kb.wms.inbound.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SkuSupplierTest {

    @Test
    @DisplayName("SKU와 공급처를 연결해 등록한다")
    void register() {
        SkuSupplier skuSupplier = SkuSupplier.register(10L, 3L);

        assertThat(skuSupplier.getSkuId()).isEqualTo(10L);
        assertThat(skuSupplier.getSupplierId()).isEqualTo(3L);
        assertThat(skuSupplier.getSkuSupplierId()).isNull();
    }

    @Test
    @DisplayName("SKU ID나 공급처 ID가 없으면 등록할 수 없다")
    void register_missingId_throws() {
        assertThatThrownBy(() -> SkuSupplier.register(null, 3L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SkuSupplier.register(10L, null)).isInstanceOf(IllegalArgumentException.class);
    }
}

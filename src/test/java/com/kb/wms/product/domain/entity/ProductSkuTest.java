package com.kb.wms.product.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductSkuTest {

    private ProductSku sku() {
        return ProductSku.register(1L, "SKU-0001", "8800000000001", "라켓", BigDecimal.TEN,
                BigDecimal.valueOf(10000), BigDecimal.valueOf(15000), "EA", 5L);
    }

    @Test
    @DisplayName("등록에는 매입 단가와 공급 단가가 필수이고 음수는 거부한다")
    void register_requiresPrices() {
        assertThatThrownBy(() -> ProductSku.register(1L, "S", null, "이름", null, null, BigDecimal.ONE, "EA", 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProductSku.register(1L, "S", null, "이름", null, BigDecimal.ONE, null, "EA", 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProductSku.register(1L, "S", null, "이름", null, BigDecimal.valueOf(-1),
                BigDecimal.ONE, "EA", 0L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("단가 0으로도 등록할 수 있다")
    void register_zeroPrice() {
        ProductSku sku = ProductSku.register(1L, "S", null, "이름", null, BigDecimal.ZERO, BigDecimal.ZERO, "EA", 0L);

        assertThat(sku.getCurrentPurchasePrice()).isEqualByComparingTo("0");
        assertThat(sku.getCurrentSupplyPrice()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("update는 값이 있는 필드만 바꾸고 나머지와 상태·코드는 그대로 둔다")
    void update_partial() {
        ProductSku sku = sku();

        sku.update("라켓 새 이름", null, null, BigDecimal.valueOf(12000), null, 9L);

        assertThat(sku.getName()).isEqualTo("라켓 새 이름");
        assertThat(sku.getCurrentPurchasePrice()).isEqualByComparingTo("12000");
        assertThat(sku.getCurrentSupplyPrice()).isEqualByComparingTo("15000");
        assertThat(sku.getSafetyStockQuantity()).isEqualTo(9L);
        assertThat(sku.getBarcode()).isEqualTo("8800000000001");
        assertThat(sku.getSkuCode()).isEqualTo("SKU-0001");
        assertThat(sku.isActive()).isTrue();
    }

    @Test
    @DisplayName("update에서 음수 단가는 거부한다")
    void update_negativePrice() {
        ProductSku sku = sku();

        assertThatThrownBy(() -> sku.update(null, null, null, BigDecimal.valueOf(-1), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sku.update(null, null, null, null, BigDecimal.valueOf(-1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.kb.wms.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kb.wms.product.application.port.in.result.CodeItem;

class ProductCodeServiceTest {

    private final ProductCodeService service = new ProductCodeService();

    @Test
    @DisplayName("SKU 단위 목록은 EA, BOX, SET을 코드와 이름으로 돌려준다")
    void getSkuUnits() {
        assertThat(service.getSkuUnits()).containsExactly(
                new CodeItem("EA", "개"),
                new CodeItem("BOX", "박스"),
                new CodeItem("SET", "세트"));
    }
}

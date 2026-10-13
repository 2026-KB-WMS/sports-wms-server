package com.kb.wms.product.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.product.application.port.in.ProductCodeUseCase;
import com.kb.wms.product.application.port.in.result.CodeItem;

@WebMvcTest(ProductCodeController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductCodeUseCase productCodeUseCase;

    @Test
    @DisplayName("SKU 단위 목록을 data.items로 반환한다")
    void getSkuUnits() throws Exception {
        when(productCodeUseCase.getSkuUnits()).thenReturn(List.of(
                new CodeItem("EA", "개"), new CodeItem("BOX", "박스"), new CodeItem("SET", "세트")));

        mockMvc.perform(get("/api/v1/products/skus/units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].code").value("EA"))
                .andExpect(jsonPath("$.data.items[0].name").value("개"))
                .andExpect(jsonPath("$.data.items[2].code").value("SET"));
    }
}

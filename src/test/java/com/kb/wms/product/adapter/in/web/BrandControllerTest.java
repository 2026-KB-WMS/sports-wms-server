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

import com.kb.wms.product.application.port.in.BrandQueryUseCase;
import com.kb.wms.product.domain.entity.Brand;

@WebMvcTest(BrandController.class)
@AutoConfigureMockMvc(addFilters = false)
class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private BrandQueryUseCase brandQueryUseCase;

    @Test
    @DisplayName("브랜드 목록을 조회하면 200과 목록을 반환한다")
    void getBrands_success() throws Exception {
        when(brandQueryUseCase.getBrands()).thenReturn(List.of(Brand.register("브랜드 A", "설명")));

        mockMvc.perform(get("/api/v1/products/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status_code").value(200))
                .andExpect(jsonPath("$.data[0].brandName").value("브랜드 A"));
    }
}

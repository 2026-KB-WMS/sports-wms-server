package com.kb.wms.product.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.BrandQueryUseCase;
import com.kb.wms.product.application.port.in.command.BrandRegisterCommand;
import com.kb.wms.product.application.port.in.query.BrandSearchCondition;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.exception.ProductErrorCode;

@WebMvcTest(BrandController.class)
@AutoConfigureMockMvc(addFilters = false)
class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean
    private BrandQueryUseCase brandQueryUseCase;

    private record TestRequest(String brandName, String description) {
    }

    @Test
    @DisplayName("브랜드 등록에 성공하면 201과 등록된 브랜드를 반환한다")
    void registerBrand_success() throws Exception {
        Brand brand = Brand.register("브랜드 A", "설명");
        when(brandQueryUseCase.registerBrand(any(BrandRegisterCommand.class))).thenReturn(brand);

        mockMvc.perform(post("/api/v1/products/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRequest("브랜드 A", "설명"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.brandName").value("브랜드 A"));
    }

    @Test
    @DisplayName("브랜드명이 중복되면 409 DUPLICATE_BRAND_NAME을 반환한다")
    void registerBrand_duplicate() throws Exception {
        when(brandQueryUseCase.registerBrand(any(BrandRegisterCommand.class)))
                .thenThrow(new BusinessException(ProductErrorCode.DUPLICATE_BRAND_NAME));

        mockMvc.perform(post("/api/v1/products/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRequest("브랜드 A", "설명"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_BRAND_NAME"));
    }

    @Test
    @DisplayName("브랜드 이름이 비어있으면 400 VALIDATION_ERROR를 반환한다")
    void registerBrand_validationError() throws Exception {
        mockMvc.perform(post("/api/v1/products/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRequest("", "설명"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("브랜드 목록을 조회하면 200과 목록을 반환한다")
    void getBrands_success() throws Exception {
        when(brandQueryUseCase.getBrands(new BrandSearchCondition(null, null)))
                .thenReturn(List.of(Brand.register("브랜드 A", "설명")));

        mockMvc.perform(get("/api/v1/products/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.items[0].brandName").value("브랜드 A"));
    }

    @Test
    @DisplayName("브랜드 목록 조회 시 keyword·isActive 파라미터가 검색 조건으로 전달된다")
    void getBrands_withFilters() throws Exception {
        when(brandQueryUseCase.getBrands(new BrandSearchCondition("브랜드", true)))
                .thenReturn(List.of(Brand.register("브랜드 A", "설명")));

        mockMvc.perform(get("/api/v1/products/brands").param("keyword", "브랜드").param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].brandName").value("브랜드 A"));
    }

    @Test
    @DisplayName("isActive 형식이 올바르지 않으면 400 VALIDATION_ERROR를 반환한다")
    void getBrands_invalidIsActive() throws Exception {
        mockMvc.perform(get("/api/v1/products/brands").param("isActive", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}

package com.kb.wms.product.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import com.kb.wms.product.application.port.in.OptionGroupUseCase;
import com.kb.wms.product.application.port.in.command.OptionGroupRegisterCommand;
import com.kb.wms.product.application.port.in.result.OptionValueSummary;
import com.kb.wms.product.application.port.in.result.ProductOptionGroupSummary;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.exception.ProductErrorCode;

@WebMvcTest(OptionGroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class OptionGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OptionGroupUseCase optionGroupUseCase;

    @Test
    @DisplayName("옵션 그룹 등록에 성공하면 201과 등록된 옵션 그룹을 반환한다")
    void registerOptionGroup_success() throws Exception {
        OptionGroup optionGroup = OptionGroup.register("색상");
        when(optionGroupUseCase.registerOptionGroup(any(OptionGroupRegisterCommand.class))).thenReturn(optionGroup);

        mockMvc.perform(post("/api/v1/products/option-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRequest("색상"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("색상"));
    }

    @Test
    @DisplayName("옵션 그룹명이 중복되면 409 DUPLICATE_OPTION_GROUP_NAME을 반환한다")
    void registerOptionGroup_duplicate() throws Exception {
        when(optionGroupUseCase.registerOptionGroup(any(OptionGroupRegisterCommand.class)))
                .thenThrow(new BusinessException(ProductErrorCode.DUPLICATE_OPTION_GROUP_NAME));

        mockMvc.perform(post("/api/v1/products/option-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRequest("색상"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_OPTION_GROUP_NAME"));
    }

    @Test
    @DisplayName("상품의 옵션 그룹·옵션 값을 조회하면 200과 그룹별 값 목록을 반환한다")
    void getOptionGroupsByProduct_success() throws Exception {
        ProductOptionGroupSummary summary = new ProductOptionGroupSummary(
                1L, "색상", List.of(new OptionValueSummary(10L, "빨강", 1)));
        when(optionGroupUseCase.getOptionGroupsByProduct(1L)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/products/{productId}/option-groups", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("색상"))
                .andExpect(jsonPath("$.data.items[0].values[0].value").value("빨강"));
    }

    @Test
    @DisplayName("존재하지 않는 상품의 옵션 그룹을 조회하면 404 PRODUCT_NOT_FOUND를 반환한다")
    void getOptionGroupsByProduct_productNotFound() throws Exception {
        when(optionGroupUseCase.getOptionGroupsByProduct(999L))
                .thenThrow(new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/products/{productId}/option-groups", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    private record TestRequest(String name) {
    }
}

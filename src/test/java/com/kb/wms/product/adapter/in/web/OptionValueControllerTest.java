package com.kb.wms.product.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.kb.wms.product.application.port.in.OptionValueUseCase;
import com.kb.wms.product.application.port.in.command.OptionValueRegisterCommand;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.exception.ProductErrorCode;

@WebMvcTest(OptionValueController.class)
@AutoConfigureMockMvc(addFilters = false)
class OptionValueControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OptionValueUseCase optionValueUseCase;

    private record TestRequest(String value, Integer sortOrder) {
    }

    @Test
    @DisplayName("옵션 값 등록에 성공하면 201과 등록된 옵션 값을 반환한다")
    void registerOptionValue_success() throws Exception {
        OptionValue optionValue = OptionValue.register(1L, "빨강", 1);
        when(optionValueUseCase.registerOptionValue(any(OptionValueRegisterCommand.class))).thenReturn(optionValue);

        mockMvc.perform(post("/api/v1/products/option-groups/{optionGroupId}/values", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRequest("빨강", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.value").value("빨강"));
    }

    @Test
    @DisplayName("존재하지 않는 옵션 그룹에 값을 등록하면 404 OPTION_GROUP_NOT_FOUND를 반환한다")
    void registerOptionValue_optionGroupNotFound() throws Exception {
        when(optionValueUseCase.registerOptionValue(any(OptionValueRegisterCommand.class)))
                .thenThrow(new BusinessException(ProductErrorCode.OPTION_GROUP_NOT_FOUND));

        mockMvc.perform(post("/api/v1/products/option-groups/{optionGroupId}/values", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRequest("빨강", 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("OPTION_GROUP_NOT_FOUND"));
    }

    @Test
    @DisplayName("옵션 값이 비어있으면 400 VALIDATION_ERROR를 반환한다")
    void registerOptionValue_validationError() throws Exception {
        mockMvc.perform(post("/api/v1/products/option-groups/{optionGroupId}/values", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRequest("", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
    }
}

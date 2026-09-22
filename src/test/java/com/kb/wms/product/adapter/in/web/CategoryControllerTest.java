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
import com.kb.wms.product.application.port.in.CategoryUseCase;
import com.kb.wms.product.application.port.in.command.CategoryRegisterCommand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.exception.ProductErrorCode;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean
    private CategoryUseCase categoryUseCase;

    @Test
    @DisplayName("카테고리 등록에 성공하면 201과 등록된 카테고리를 반환한다")
    void registerCategory_success() throws Exception {
        Category category = Category.register(null, "CAT-001", "라켓", 1, 0);
        when(categoryUseCase.registerCategory(any(CategoryRegisterCommand.class))).thenReturn(category);

        mockMvc.perform(post("/api/v1/products/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestRequest(null, "CAT-001", "라켓", 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status_code").value(201))
                .andExpect(jsonPath("$.data.categoryCode").value("CAT-001"))
                .andExpect(jsonPath("$.data.categoryName").value("라켓"));
    }

    @Test
    @DisplayName("카테고리 코드가 비어있으면 400 VALIDATION_ERROR를 반환한다")
    void registerCategory_validationError() throws Exception {
        mockMvc.perform(post("/api/v1/products/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRequest(null, "", "라켓", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("상위 카테고리가 존재하지 않으면 404 PARENT_CATEGORY_NOT_FOUND를 반환한다")
    void registerCategory_parentNotFound() throws Exception {
        when(categoryUseCase.registerCategory(any(CategoryRegisterCommand.class)))
                .thenThrow(new BusinessException(ProductErrorCode.PARENT_CATEGORY_NOT_FOUND));

        mockMvc.perform(post("/api/v1/products/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRequest(999L, "CAT-001", "라켓", 0))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("PARENT_CATEGORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("카테고리 목록을 조회하면 200과 목록을 반환한다")
    void getCategories_success() throws Exception {
        when(categoryUseCase.getCategories()).thenReturn(List.of(Category.register(null, "CAT-001", "라켓", 1, 0)));

        mockMvc.perform(get("/api/v1/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryCode").value("CAT-001"));
    }

    private record TestRequest(Long parentCategoryId, String categoryCode, String categoryName, Integer sortOrder) {
    }
}

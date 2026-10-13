package com.kb.wms.product.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.CategoryOptionGroupUseCase;
import com.kb.wms.product.application.port.in.command.CategoryOptionGroupAssignCommand;
import com.kb.wms.product.application.port.in.result.CategoryOptionGroupSummary;
import com.kb.wms.product.application.port.in.result.OptionValueSummary;
import com.kb.wms.product.domain.enums.OptionType;
import com.kb.wms.product.exception.ProductErrorCode;

@WebMvcTest(CategoryOptionGroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryOptionGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryOptionGroupUseCase categoryOptionGroupUseCase;

    private CategoryOptionGroupSummary summary(Long groupId, OptionType type, Long definedCategoryId) {
        return new CategoryOptionGroupSummary(groupId, "무게", type, 0, definedCategoryId,
                List.of(new OptionValueSummary(100L, "3U", "3U", 1, true)));
    }

    @Test
    @DisplayName("지정을 바꾸면 200과 상속분을 포함한 적용 결과를 반환하고, 상위에서 온 지정은 inherited=true다")
    void assignOptionGroups_success() throws Exception {
        when(categoryOptionGroupUseCase.assignOptionGroups(any(CategoryOptionGroupAssignCommand.class)))
                .thenReturn(List.of(summary(10L, OptionType.SKU, 3L), summary(11L, OptionType.SPEC, 1L)));

        mockMvc.perform(put("/api/v1/products/categories/{categoryId}/option-groups", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"optionGroupId\":10,\"optionType\":\"SKU\",\"sortOrder\":0}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].optionType").value("SKU"))
                .andExpect(jsonPath("$.data.items[0].inherited").value(false))
                .andExpect(jsonPath("$.data.items[1].optionType").value("SPEC"))
                .andExpect(jsonPath("$.data.items[1].inherited").value(true))
                .andExpect(jsonPath("$.data.items[1].definedCategoryId").value(1))
                .andExpect(jsonPath("$.data.items[0].values[0].optionCode").value("3U"));
    }

    @Test
    @DisplayName("빈 items는 허용하고, items가 없거나 용도·그룹 ID가 빠지면 400 VALIDATION_ERROR다")
    void assignOptionGroups_validation() throws Exception {
        when(categoryOptionGroupUseCase.assignOptionGroups(any(CategoryOptionGroupAssignCommand.class)))
                .thenReturn(List.of());

        mockMvc.perform(put("/api/v1/products/categories/{categoryId}/option-groups", 3L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isOk());

        for (String body : new String[] {
                "{}",
                "{\"items\":[{\"optionGroupId\":10,\"sortOrder\":0}]}",
                "{\"items\":[{\"optionType\":\"SKU\"}]}",
                "{\"items\":[{\"optionGroupId\":10,\"optionType\":\"SKU\",\"sortOrder\":-1}]}"}) {
            mockMvc.perform(put("/api/v1/products/categories/{categoryId}/option-groups", 3L)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
    }

    @Test
    @DisplayName("용도가 SPEC·SKU가 아니면 400이다")
    void assignOptionGroups_invalidType() throws Exception {
        mockMvc.perform(put("/api/v1/products/categories/{categoryId}/option-groups", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"optionGroupId\":10,\"optionType\":\"ETC\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("없는 카테고리면 404 CATEGORY_NOT_FOUND다")
    void assignOptionGroups_categoryNotFound() throws Exception {
        when(categoryOptionGroupUseCase.assignOptionGroups(any(CategoryOptionGroupAssignCommand.class)))
                .thenThrow(new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));

        mockMvc.perform(put("/api/v1/products/categories/{categoryId}/option-groups", 999L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("적용되는 옵션 그룹을 조회한다")
    void getOptionGroups_success() throws Exception {
        when(categoryOptionGroupUseCase.getOptionGroups(3L))
                .thenReturn(List.of(summary(10L, OptionType.SKU, 3L)));

        mockMvc.perform(get("/api/v1/products/categories/{categoryId}/option-groups", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].optionGroupId").value(10))
                .andExpect(jsonPath("$.data.items[0].name").value("무게"))
                .andExpect(jsonPath("$.data.items[0].values[0].isActive").value(true));
    }
}

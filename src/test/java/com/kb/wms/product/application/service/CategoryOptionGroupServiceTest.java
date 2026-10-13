package com.kb.wms.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.command.CategoryOptionGroupAssignCommand;
import com.kb.wms.product.application.port.in.result.CategoryOptionGroupSummary;
import com.kb.wms.product.application.port.out.CategoryOptionGroupRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.CategoryOptionGroup;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.domain.enums.OptionType;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class CategoryOptionGroupServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private OptionGroupRepository optionGroupRepository;
    @Mock
    private OptionValueRepository optionValueRepository;
    @Mock
    private CategoryOptionGroupRepository categoryOptionGroupRepository;
    @Mock
    private CategoryOptionResolver categoryOptionResolver;

    @InjectMocks
    private CategoryOptionGroupService service;

    private final Category category = Category.register(null, "RACKET", "라켓", 1, 0);

    @Test
    @DisplayName("카테고리의 지정을 목록으로 통째로 바꾸고 상속분까지 합친 결과를 돌려준다")
    void assignOptionGroups_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(optionGroupRepository.findById(10L))
                .thenReturn(Optional.of(OptionGroup.builder().optionGroupId(10L).name("무게").build()));
        when(optionGroupRepository.findById(11L))
                .thenReturn(Optional.of(OptionGroup.builder().optionGroupId(11L).name("밸런스").build()));
        CategoryOptionGroup weight = CategoryOptionGroup.assign(1L, 10L, OptionType.SKU, 0);
        when(categoryOptionResolver.effectiveMappings(1L)).thenReturn(List.of(weight));
        when(optionGroupRepository.findAllByIds(List.of(10L)))
                .thenReturn(List.of(OptionGroup.builder().optionGroupId(10L).name("무게").build()));
        when(optionValueRepository.findByOptionGroupId(10L)).thenReturn(List.of(
                OptionValue.builder().optionValueId(100L).optionGroupId(10L).optionCode("3U").value("3U")
                        .sortOrder(2).build(),
                OptionValue.builder().optionValueId(101L).optionGroupId(10L).optionCode("4U").value("4U")
                        .sortOrder(1).build()));

        List<CategoryOptionGroupSummary> result = service.assignOptionGroups(new CategoryOptionGroupAssignCommand(
                1L, List.of(new CategoryOptionGroupAssignCommand.Item(10L, OptionType.SKU, 0),
                        new CategoryOptionGroupAssignCommand.Item(11L, OptionType.SPEC, 1))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CategoryOptionGroup>> saved = ArgumentCaptor.forClass(List.class);
        verify(categoryOptionGroupRepository).replaceAll(org.mockito.ArgumentMatchers.eq(1L), saved.capture());
        assertThat(saved.getValue()).extracting(CategoryOptionGroup::getOptionGroupId).containsExactly(10L, 11L);
        assertThat(saved.getValue().get(1).getOptionType()).isEqualTo(OptionType.SPEC);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("무게");
        assertThat(result.get(0).values()).extracting(v -> v.optionCode()).containsExactly("4U", "3U");
    }

    @Test
    @DisplayName("없는 카테고리면 CATEGORY_NOT_FOUND이고 저장하지 않는다")
    void assignOptionGroups_categoryNotFound() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignOptionGroups(new CategoryOptionGroupAssignCommand(9L, List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.CATEGORY_NOT_FOUND.name());
        verify(categoryOptionGroupRepository, never()).replaceAll(any(), any());
    }

    @Test
    @DisplayName("없는 옵션 그룹이면 OPTION_GROUP_NOT_FOUND이고 저장하지 않는다")
    void assignOptionGroups_groupNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(optionGroupRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignOptionGroups(new CategoryOptionGroupAssignCommand(
                1L, List.of(new CategoryOptionGroupAssignCommand.Item(10L, OptionType.SKU, 0)))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.OPTION_GROUP_NOT_FOUND.name());
        verify(categoryOptionGroupRepository, never()).replaceAll(any(), any());
    }

    @Test
    @DisplayName("같은 옵션 그룹을 두 번 지정하면 VALIDATION_ERROR이고 저장하지 않는다")
    void assignOptionGroups_duplicateGroup() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(optionGroupRepository.findById(10L))
                .thenReturn(Optional.of(OptionGroup.builder().optionGroupId(10L).name("무게").build()));

        assertThatThrownBy(() -> service.assignOptionGroups(new CategoryOptionGroupAssignCommand(
                1L, List.of(new CategoryOptionGroupAssignCommand.Item(10L, OptionType.SKU, 0),
                        new CategoryOptionGroupAssignCommand.Item(10L, OptionType.SPEC, 1)))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(categoryOptionGroupRepository, never()).replaceAll(any(), any());
    }
}

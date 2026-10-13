package com.kb.wms.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.out.CategoryOptionGroupRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.CategoryOptionGroup;
import com.kb.wms.product.domain.enums.OptionType;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class CategoryOptionResolverTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryOptionGroupRepository categoryOptionGroupRepository;

    @InjectMocks
    private CategoryOptionResolver resolver;

    private Category category(Long id, Long parentId) {
        return Category.builder().categoryId(id).parentCategoryId(parentId).categoryCode("C" + id).name("c" + id).build();
    }

    @Test
    @DisplayName("상위 카테고리의 지정을 상속하고, 같은 그룹은 가까운 카테고리의 지정이 우선한다")
    void effectiveMappings_inheritsAndNearestWins() {
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category(3L, 2L)));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category(2L, 1L)));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, null)));
        when(categoryOptionGroupRepository.findByCategoryIds(List.of(1L, 2L, 3L))).thenReturn(List.of(
                CategoryOptionGroup.assign(1L, 10L, OptionType.SKU, 0),
                CategoryOptionGroup.assign(1L, 11L, OptionType.SPEC, 1),
                CategoryOptionGroup.assign(2L, 10L, OptionType.SPEC, 0),
                CategoryOptionGroup.assign(3L, 12L, OptionType.SKU, 0)));

        List<CategoryOptionGroup> result = resolver.effectiveMappings(3L);

        assertThat(result).extracting(CategoryOptionGroup::getOptionGroupId).containsExactly(10L, 12L, 11L);
        assertThat(result.get(0).getOptionType()).isEqualTo(OptionType.SPEC);
        assertThat(result.get(0).getCategoryId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("지정이 없으면 빈 목록이다")
    void effectiveMappings_empty() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, null)));
        when(categoryOptionGroupRepository.findByCategoryIds(List.of(1L))).thenReturn(List.of());

        assertThat(resolver.effectiveMappings(1L)).isEmpty();
    }

    @Test
    @DisplayName("카테고리가 없으면 CATEGORY_NOT_FOUND 예외를 던진다")
    void effectiveMappings_notFound() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.effectiveMappings(9L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.CATEGORY_NOT_FOUND.name());
    }
}

package com.kb.wms.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.result.ProductSpecSummary;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.domain.entity.CategoryOptionGroup;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.domain.enums.OptionType;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class OptionSelectionResolverTest {

    @Mock
    private OptionValueRepository optionValueRepository;
    @Mock
    private OptionGroupRepository optionGroupRepository;

    @InjectMocks
    private OptionSelectionResolver resolver;

    // 그룹 1(무게, SKU, 순서 0), 그룹 2(그립, SKU, 순서 1), 그룹 3(밸런스, SPEC)
    private final List<CategoryOptionGroup> mappings = List.of(
            CategoryOptionGroup.assign(1L, 1L, OptionType.SKU, 0),
            CategoryOptionGroup.assign(1L, 2L, OptionType.SKU, 1),
            CategoryOptionGroup.assign(1L, 3L, OptionType.SPEC, 2));

    private OptionValue value(Long id, Long groupId, String code, String value) {
        return OptionValue.builder().optionValueId(id).optionGroupId(groupId).optionCode(code).value(value)
                .sortOrder(1).build();
    }

    private void assertError(Runnable call, ProductErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(expected.name());
    }

    @Test
    @DisplayName("지정된 그룹마다 값이 하나씩 있으면 카테고리 지정 순서대로 돌려준다")
    void resolve_success_sortedByMappingOrder() {
        when(optionValueRepository.findAllByIds(List.of(20L, 10L))).thenReturn(List.of(
                value(20L, 2L, "G4", "G4"), value(10L, 1L, "3U", "3U")));

        List<OptionValue> result = resolver.resolve(List.of(20L, 10L), mappings, OptionType.SKU);

        assertThat(result).extracting(OptionValue::getOptionValueId).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("용도가 SPEC이면 SPEC 그룹만 검사하고 SKU 그룹은 요구하지 않는다")
    void resolve_specOnlyChecksSpecGroups() {
        when(optionValueRepository.findAllByIds(List.of(30L))).thenReturn(List.of(value(30L, 3L, "HEAD", "헤드 헤비")));

        assertThat(resolver.resolve(List.of(30L), mappings, OptionType.SPEC)).hasSize(1);
    }

    @Test
    @DisplayName("지정이 없는 카테고리는 선택이 비어 있어도 통과한다")
    void resolve_noMappingsAllowsEmpty() {
        assertThat(resolver.resolve(null, List.of(), OptionType.SKU)).isEmpty();
        assertThat(resolver.resolve(List.of(), List.of(), OptionType.SKU)).isEmpty();
    }

    @Test
    @DisplayName("같은 옵션 값 ID를 두 번 보내면 DUPLICATE_OPTION_VALUE다")
    void resolve_duplicateIds() {
        assertError(() -> resolver.resolve(List.of(10L, 10L), mappings, OptionType.SKU),
                ProductErrorCode.DUPLICATE_OPTION_VALUE);
    }

    @Test
    @DisplayName("없는 옵션 값이면 OPTION_VALUE_NOT_FOUND다")
    void resolve_notFound() {
        when(optionValueRepository.findAllByIds(List.of(99L))).thenReturn(List.of());

        assertError(() -> resolver.resolve(List.of(99L), mappings, OptionType.SKU),
                ProductErrorCode.OPTION_VALUE_NOT_FOUND);
    }

    @Test
    @DisplayName("비활성 옵션 값이면 OPTION_VALUE_INACTIVE다")
    void resolve_inactive() {
        OptionValue inactive = value(10L, 1L, "3U", "3U");
        inactive.deactivate();
        when(optionValueRepository.findAllByIds(List.of(10L))).thenReturn(List.of(inactive));

        assertError(() -> resolver.resolve(List.of(10L), mappings, OptionType.SKU),
                ProductErrorCode.OPTION_VALUE_INACTIVE);
    }

    @Test
    @DisplayName("카테고리에 지정되지 않은 그룹의 값이면 OPTION_NOT_ALLOWED_FOR_CATEGORY다")
    void resolve_groupNotMapped() {
        when(optionValueRepository.findAllByIds(List.of(40L))).thenReturn(List.of(value(40L, 4L, "RED", "빨강")));

        assertError(() -> resolver.resolve(List.of(40L), mappings, OptionType.SKU),
                ProductErrorCode.OPTION_NOT_ALLOWED_FOR_CATEGORY);
    }

    @Test
    @DisplayName("SPEC 그룹의 값을 SKU 옵션으로 보내면 OPTION_NOT_ALLOWED_FOR_CATEGORY다")
    void resolve_wrongType() {
        when(optionValueRepository.findAllByIds(List.of(30L))).thenReturn(List.of(value(30L, 3L, "HEAD", "헤드 헤비")));

        assertError(() -> resolver.resolve(List.of(30L), mappings, OptionType.SKU),
                ProductErrorCode.OPTION_NOT_ALLOWED_FOR_CATEGORY);
    }

    @Test
    @DisplayName("같은 그룹의 값을 둘 보내면 OPTION_GROUP_CONFLICT다")
    void resolve_groupConflict() {
        when(optionValueRepository.findAllByIds(List.of(10L, 11L))).thenReturn(List.of(
                value(10L, 1L, "3U", "3U"), value(11L, 1L, "4U", "4U")));

        assertError(() -> resolver.resolve(List.of(10L, 11L), mappings, OptionType.SKU),
                ProductErrorCode.OPTION_GROUP_CONFLICT);
    }

    @Test
    @DisplayName("지정된 그룹의 값이 빠지면 REQUIRED_OPTION_MISSING이고 메시지에 누락된 그룹명이 들어간다")
    void resolve_missingRequiredGroup() {
        when(optionValueRepository.findAllByIds(List.of(10L))).thenReturn(List.of(value(10L, 1L, "3U", "3U")));
        when(optionGroupRepository.findAllByIds(List.of(2L)))
                .thenReturn(List.of(OptionGroup.builder().optionGroupId(2L).name("그립").build()));

        assertThatThrownBy(() -> resolver.resolve(List.of(10L), mappings, OptionType.SKU))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("그립")
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.REQUIRED_OPTION_MISSING.name());
    }

    @Test
    @DisplayName("describe는 옵션 그룹명을 붙여 옵션 그룹 ID 순으로 돌려준다")
    void describe_attachesGroupNames() {
        when(optionValueRepository.findAllByIds(List.of(30L, 10L))).thenReturn(List.of(
                value(30L, 3L, "HEAD", "헤드 헤비"), value(10L, 1L, "3U", "3U")));
        when(optionGroupRepository.findAllByIds(List.of(3L, 1L))).thenReturn(List.of(
                OptionGroup.builder().optionGroupId(1L).name("무게").build(),
                OptionGroup.builder().optionGroupId(3L).name("밸런스").build()));

        List<ProductSpecSummary> result = resolver.describe(List.of(30L, 10L));

        assertThat(result).extracting(ProductSpecSummary::optionGroupName).containsExactly("무게", "밸런스");
        assertThat(result.get(1).optionCode()).isEqualTo("HEAD");
    }
}

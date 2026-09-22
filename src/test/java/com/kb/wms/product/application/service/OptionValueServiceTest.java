package com.kb.wms.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.command.OptionValueRegisterCommand;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class OptionValueServiceTest {

    @Mock
    private OptionValueRepository optionValueRepository;
    @Mock
    private OptionGroupRepository optionGroupRepository;

    @InjectMocks
    private OptionValueService optionValueService;

    @Test
    @DisplayName("옵션 그룹이 존재하고 값이 중복되지 않으면 옵션 값 등록에 성공한다")
    void registerOptionValue_success() {
        Long optionGroupId = 1L;
        when(optionGroupRepository.findById(optionGroupId))
                .thenReturn(Optional.of(OptionGroup.builder().optionGroupId(optionGroupId).name("색상").build()));
        when(optionValueRepository.existsByOptionGroupIdAndValue(optionGroupId, "빨강")).thenReturn(false);
        when(optionValueRepository.save(any(OptionValue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OptionValue result = optionValueService.registerOptionValue(
                new OptionValueRegisterCommand(optionGroupId, "빨강", 1));

        assertThat(result.getValue()).isEqualTo("빨강");
        assertThat(result.getOptionGroupId()).isEqualTo(optionGroupId);
    }

    @Test
    @DisplayName("옵션 그룹이 존재하지 않으면 OPTION_GROUP_NOT_FOUND 예외를 던진다")
    void registerOptionValue_optionGroupNotFound() {
        when(optionGroupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> optionValueService.registerOptionValue(
                new OptionValueRegisterCommand(999L, "빨강", 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.OPTION_GROUP_NOT_FOUND.name());
        verify(optionValueRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 옵션 그룹에 동일한 값이 이미 존재하면 DUPLICATE_OPTION_VALUE 예외를 던진다")
    void registerOptionValue_duplicateValue() {
        Long optionGroupId = 1L;
        when(optionGroupRepository.findById(optionGroupId))
                .thenReturn(Optional.of(OptionGroup.builder().optionGroupId(optionGroupId).name("색상").build()));
        when(optionValueRepository.existsByOptionGroupIdAndValue(optionGroupId, "빨강")).thenReturn(true);

        assertThatThrownBy(() -> optionValueService.registerOptionValue(
                new OptionValueRegisterCommand(optionGroupId, "빨강", 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_OPTION_VALUE.name());
        verify(optionValueRepository, never()).save(any());
    }
}

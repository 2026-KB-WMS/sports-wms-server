package com.kb.wms.product.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.OptionValueUseCase;
import com.kb.wms.product.application.port.in.command.OptionValueRegisterCommand;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.domain.entity.OptionValue;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptionValueService implements OptionValueUseCase {

    private final OptionValueRepository optionValueRepository;
    private final OptionGroupRepository optionGroupRepository;

    @Override
    @Transactional
    public OptionValue registerOptionValue(OptionValueRegisterCommand command) {
        if (!optionGroupRepository.findById(command.optionGroupId()).isPresent()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "옵션 그룹을 찾을 수 없습니다.");
        }
        if (optionValueRepository.existsByOptionGroupIdAndValue(command.optionGroupId(), command.value())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 존재하는 옵션 값입니다.");
        }

        OptionValue optionValue = OptionValue.register(
                command.optionGroupId(), command.value(), command.sortOrder());
        return optionValueRepository.save(optionValue);
    }
}

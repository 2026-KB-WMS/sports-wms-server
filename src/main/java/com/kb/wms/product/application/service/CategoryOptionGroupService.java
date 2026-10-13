package com.kb.wms.product.application.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.CategoryOptionGroupUseCase;
import com.kb.wms.product.application.port.in.command.CategoryOptionGroupAssignCommand;
import com.kb.wms.product.application.port.in.result.CategoryOptionGroupSummary;
import com.kb.wms.product.application.port.in.result.OptionValueSummary;
import com.kb.wms.product.application.port.out.CategoryOptionGroupRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.domain.entity.CategoryOptionGroup;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryOptionGroupService implements CategoryOptionGroupUseCase {

    private final CategoryRepository categoryRepository;
    private final OptionGroupRepository optionGroupRepository;
    private final OptionValueRepository optionValueRepository;
    private final CategoryOptionGroupRepository categoryOptionGroupRepository;
    private final CategoryOptionResolver categoryOptionResolver;

    @Override
    @Transactional
    public List<CategoryOptionGroupSummary> assignOptionGroups(CategoryOptionGroupAssignCommand command) {
        if (categoryRepository.findById(command.categoryId()).isEmpty()) {
            throw new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND);
        }
        Set<Long> groupIds = new HashSet<>();
        for (CategoryOptionGroupAssignCommand.Item item : command.items()) {
            if (!groupIds.add(item.optionGroupId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "같은 옵션 그룹을 두 번 지정할 수 없습니다.");
            }
            if (optionGroupRepository.findById(item.optionGroupId()).isEmpty()) {
                throw new BusinessException(ProductErrorCode.OPTION_GROUP_NOT_FOUND);
            }
        }

        categoryOptionGroupRepository.replaceAll(command.categoryId(), command.items().stream()
                .map(item -> CategoryOptionGroup.assign(
                        command.categoryId(), item.optionGroupId(), item.optionType(), item.sortOrder()))
                .toList());
        return getOptionGroups(command.categoryId());
    }

    @Override
    public List<CategoryOptionGroupSummary> getOptionGroups(Long categoryId) {
        List<CategoryOptionGroup> mappings = categoryOptionResolver.effectiveMappings(categoryId);
        Map<Long, OptionGroup> groups = optionGroupRepository.findAllByIds(
                        mappings.stream().map(CategoryOptionGroup::getOptionGroupId).toList()).stream()
                .collect(Collectors.toMap(OptionGroup::getOptionGroupId, Function.identity()));
        return mappings.stream()
                .map(mapping -> new CategoryOptionGroupSummary(
                        mapping.getOptionGroupId(),
                        groups.containsKey(mapping.getOptionGroupId())
                                ? groups.get(mapping.getOptionGroupId()).getName() : null,
                        mapping.getOptionType(),
                        mapping.getSortOrder(),
                        mapping.getCategoryId(),
                        optionValueRepository.findByOptionGroupId(mapping.getOptionGroupId()).stream()
                                .sorted(Comparator.comparingInt(
                                        com.kb.wms.product.domain.entity.OptionValue::getSortOrder))
                                .map(value -> new OptionValueSummary(value.getOptionValueId(),
                                        value.getOptionCode(), value.getValue(), value.getSortOrder(),
                                        value.isActive()))
                                .toList()))
                .toList();
    }
}

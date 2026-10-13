package com.kb.wms.product.application.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.result.ProductSpecSummary;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.domain.entity.CategoryOptionGroup;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.domain.enums.OptionType;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 요청에서 선택한 옵션 값이 카테고리 지정과 맞는지 검증하고, 지정 순서대로 정렬해 돌려준다(ADR-025).
 */
@Component
@RequiredArgsConstructor
class OptionSelectionResolver {

    private final OptionValueRepository optionValueRepository;
    private final OptionGroupRepository optionGroupRepository;

    /**
     * 검증 순서: 중복 ID(409), 없는 값(404), 비활성 값(409), 카테고리에 없거나 용도가 다른 그룹(400),
     * 같은 그룹 값 둘 이상(409), 지정된 그룹 누락(400). 결과는 카테고리 지정 순서다.
     */
    List<OptionValue> resolve(List<Long> optionValueIds, List<CategoryOptionGroup> mappings, OptionType type) {
        List<Long> ids = optionValueIds == null ? List.of() : optionValueIds;
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_OPTION_VALUE);
        }

        Map<Long, Integer> orderByGroup = new java.util.HashMap<>();
        List<CategoryOptionGroup> typed = mappings.stream().filter(m -> m.getOptionType() == type).toList();
        for (int i = 0; i < typed.size(); i++) {
            orderByGroup.put(typed.get(i).getOptionGroupId(), i);
        }

        Map<Long, OptionValue> found = optionValueRepository.findAllByIds(ids).stream()
                .collect(Collectors.toMap(OptionValue::getOptionValueId, Function.identity()));
        Set<Long> usedGroupIds = new LinkedHashSet<>();
        List<OptionValue> selected = new ArrayList<>();
        for (Long id : ids) {
            OptionValue optionValue = found.get(id);
            if (optionValue == null) {
                throw new BusinessException(ProductErrorCode.OPTION_VALUE_NOT_FOUND);
            }
            if (!optionValue.isActive()) {
                throw new BusinessException(ProductErrorCode.OPTION_VALUE_INACTIVE);
            }
            if (!orderByGroup.containsKey(optionValue.getOptionGroupId())) {
                throw new BusinessException(ProductErrorCode.OPTION_NOT_ALLOWED_FOR_CATEGORY);
            }
            if (!usedGroupIds.add(optionValue.getOptionGroupId())) {
                throw new BusinessException(ProductErrorCode.OPTION_GROUP_CONFLICT);
            }
            selected.add(optionValue);
        }

        List<Long> missingGroupIds = typed.stream()
                .map(CategoryOptionGroup::getOptionGroupId)
                .filter(groupId -> !usedGroupIds.contains(groupId))
                .toList();
        if (!missingGroupIds.isEmpty()) {
            String names = optionGroupRepository.findAllByIds(missingGroupIds).stream()
                    .map(OptionGroup::getName)
                    .collect(Collectors.joining(", "));
            throw new BusinessException(ProductErrorCode.REQUIRED_OPTION_MISSING,
                    "필수 옵션 그룹의 값이 없습니다: " + names);
        }

        selected.sort(Comparator.comparingInt(optionValue -> orderByGroup.get(optionValue.getOptionGroupId())));
        return selected;
    }

    /** 스펙 값 ID들을 옵션 그룹명까지 붙여 옵션 그룹 ID 오름차순으로 돌려준다. */
    List<ProductSpecSummary> describe(Collection<Long> optionValueIds) {
        List<OptionValue> values = optionValueRepository.findAllByIds(optionValueIds);
        Map<Long, OptionGroup> groups = optionGroupRepository.findAllByIds(
                        values.stream().map(OptionValue::getOptionGroupId).distinct().toList()).stream()
                .collect(Collectors.toMap(OptionGroup::getOptionGroupId, Function.identity()));
        return values.stream()
                .sorted(Comparator.comparing(OptionValue::getOptionGroupId))
                .map(value -> {
                    OptionGroup group = groups.get(value.getOptionGroupId());
                    return new ProductSpecSummary(value.getOptionGroupId(),
                            group != null ? group.getName() : null,
                            value.getOptionValueId(), value.getOptionCode(), value.getValue());
                })
                .toList();
    }
}

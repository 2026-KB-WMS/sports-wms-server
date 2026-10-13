package com.kb.wms.product.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.out.CategoryOptionGroupRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.CategoryOptionGroup;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 카테고리에 실제로 적용되는 옵션 그룹 지정을 계산한다(ADR-025).
 * 루트부터 해당 카테고리까지의 지정을 합치고, 같은 그룹이 여러 단계에 있으면 가까운 카테고리가 우선한다.
 */
@Component
@RequiredArgsConstructor
class CategoryOptionResolver {

    private final CategoryRepository categoryRepository;
    private final CategoryOptionGroupRepository categoryOptionGroupRepository;

    /** 노출 순서(sortOrder), 같으면 옵션 그룹 ID 오름차순. 카테고리가 없으면 CATEGORY_NOT_FOUND. */
    List<CategoryOptionGroup> effectiveMappings(Long categoryId) {
        List<Long> chain = new ArrayList<>();
        Long current = categoryId;
        while (current != null) {
            Category category = categoryRepository.findById(current)
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));
            chain.add(category.getCategoryId());
            current = category.getParentCategoryId();
        }
        Collections.reverse(chain);

        Map<Long, List<CategoryOptionGroup>> byCategory = new LinkedHashMap<>();
        for (CategoryOptionGroup mapping : categoryOptionGroupRepository.findByCategoryIds(chain)) {
            byCategory.computeIfAbsent(mapping.getCategoryId(), id -> new ArrayList<>()).add(mapping);
        }
        Map<Long, CategoryOptionGroup> merged = new LinkedHashMap<>();
        for (Long id : chain) {
            for (CategoryOptionGroup mapping : byCategory.getOrDefault(id, List.of())) {
                merged.put(mapping.getOptionGroupId(), mapping);
            }
        }
        return merged.values().stream()
                .sorted(Comparator.comparingInt(CategoryOptionGroup::getSortOrder)
                        .thenComparing(CategoryOptionGroup::getOptionGroupId))
                .toList();
    }
}

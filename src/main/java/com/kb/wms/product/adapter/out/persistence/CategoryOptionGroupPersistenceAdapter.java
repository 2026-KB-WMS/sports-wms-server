package com.kb.wms.product.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.kb.wms.product.adapter.out.persistence.entity.CategoryOptionGroupJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.CategoryOptionGroupJpaRepository;
import com.kb.wms.product.application.port.out.CategoryOptionGroupRepository;
import com.kb.wms.product.domain.entity.CategoryOptionGroup;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CategoryOptionGroupPersistenceAdapter implements CategoryOptionGroupRepository {

    private final CategoryOptionGroupJpaRepository categoryOptionGroupJpaRepository;

    @Override
    public List<CategoryOptionGroup> findByCategoryIds(Collection<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return categoryOptionGroupJpaRepository.findByCategoryIdIn(categoryIds).stream()
                .map(CategoryOptionGroupJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void replaceAll(Long categoryId, List<CategoryOptionGroup> mappings) {
        categoryOptionGroupJpaRepository.deleteByCategoryId(categoryId);
        categoryOptionGroupJpaRepository.saveAll(
                mappings.stream().map(CategoryOptionGroupJpaEntity::fromDomain).toList());
    }
}

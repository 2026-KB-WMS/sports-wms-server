package com.kb.wms.product.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.adapter.out.persistence.entity.CategoryJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.CategoryJpaRepository;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.product.application.port.in.query.CategorySearchCondition;
import com.kb.wms.common.persistence.SearchKeyword;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CategoryPersistenceAdapter implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public Category save(Category category) {
        CategoryJpaEntity saved = categoryJpaRepository.save(CategoryJpaEntity.fromDomain(category));
        return saved.toDomain();
    }

    @Override
    public Optional<Category> findById(Long categoryId) {
        return categoryJpaRepository.findById(categoryId).map(CategoryJpaEntity::toDomain);
    }

    @Override
    public List<Category> search(CategorySearchCondition condition) {
        return categoryJpaRepository.search(
                        condition.parentCategoryId(),
                        condition.depth(),
                        SearchKeyword.normalize(condition.keyword()),
                        ProductStatus.fromActiveFlag(condition.isActive()))
                .stream()
                .map(CategoryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCategoryCode(String categoryCode) {
        return categoryJpaRepository.existsByCategoryCode(categoryCode);
    }
}

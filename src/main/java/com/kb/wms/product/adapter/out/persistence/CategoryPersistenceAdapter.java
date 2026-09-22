package com.kb.wms.product.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.adapter.out.persistence.entity.CategoryJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.CategoryJpaRepository;
import com.kb.wms.product.domain.entity.Category;

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
    public List<Category> findAll() {
        return categoryJpaRepository.findAll().stream()
                .map(CategoryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCategoryCode(String categoryCode) {
        return categoryJpaRepository.existsByCategoryCode(categoryCode);
    }
}

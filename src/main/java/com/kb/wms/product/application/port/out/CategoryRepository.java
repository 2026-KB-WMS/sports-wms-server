package com.kb.wms.product.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.product.domain.entity.Category;

/**
 * 카테고리 영속성 아웃바운드 포트.
 */
public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(Long categoryId);

    List<Category> findAll();

    boolean existsByCategoryCode(String categoryCode);
}

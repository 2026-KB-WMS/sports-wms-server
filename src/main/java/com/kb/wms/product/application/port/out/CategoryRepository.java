package com.kb.wms.product.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.product.application.port.in.query.CategorySearchCondition;
import com.kb.wms.product.domain.entity.Category;

/**
 * 카테고리 영속성 아웃바운드 포트.
 */
public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(Long categoryId);

    /** 없는 ID는 결과에서 빠진다. 목록에서 카테고리명을 한 번에 읽을 때 쓴다. */
    List<Category> findByIdIn(java.util.Collection<Long> categoryIds);

    /** 카테고리 정렬 순서(sortOrder) 오름차순 */
    List<Category> search(CategorySearchCondition condition);

    boolean existsByCategoryCode(String categoryCode);
}

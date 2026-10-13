package com.kb.wms.product.application.port.out;

import java.util.Collection;
import java.util.List;

import com.kb.wms.product.domain.entity.CategoryOptionGroup;

/**
 * 카테고리-옵션 그룹 매핑 영속성 아웃바운드 포트.
 */
public interface CategoryOptionGroupRepository {

    /** 여러 카테고리의 매핑을 쿼리 한 번으로 조회한다. */
    List<CategoryOptionGroup> findByCategoryIds(Collection<Long> categoryIds);

    /** 카테고리의 매핑을 모두 지우고 주어진 목록으로 바꾼다. */
    void replaceAll(Long categoryId, List<CategoryOptionGroup> mappings);
}

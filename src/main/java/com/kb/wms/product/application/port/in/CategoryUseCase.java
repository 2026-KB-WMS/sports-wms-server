package com.kb.wms.product.application.port.in;

import com.kb.wms.product.application.port.in.query.CategorySearchCondition;
import java.util.List;

import com.kb.wms.product.application.port.in.command.CategoryRegisterCommand;
import com.kb.wms.product.application.port.in.command.CategoryUpdateCommand;
import com.kb.wms.product.domain.entity.Category;

/**
 * 카테고리 등록/수정/조회 유스케이스. POST, GET /api/v1/products/categories, PATCH /api/v1/products/categories/{categoryId}
 */
public interface CategoryUseCase {

    Category registerCategory(CategoryRegisterCommand command);

    /** 이름·정렬 순서를 부분 수정한다. 없으면 CATEGORY_NOT_FOUND. */
    Category updateCategory(CategoryUpdateCommand command);

    /** 필터의 상위 카테고리가 없으면 CATEGORY_NOT_FOUND */
    List<Category> getCategories(CategorySearchCondition condition);

    Category getCategory(Long categoryId);

    /** 카테고리 ID별 이름. 목록 응답에서 항목마다 조회하지 않도록 한 번에 읽는다. 없는 ID는 맵에 없다. */
    java.util.Map<Long, String> getCategoryNames(java.util.Collection<Long> categoryIds);
}

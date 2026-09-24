package com.kb.wms.product.application.port.in;

import com.kb.wms.product.application.port.in.query.CategorySearchCondition;
import java.util.List;

import com.kb.wms.product.application.port.in.command.CategoryRegisterCommand;
import com.kb.wms.product.domain.entity.Category;

/**
 * 카테고리 등록/조회 유스케이스. POST, GET /api/v1/products/categories
 */
public interface CategoryUseCase {

    Category registerCategory(CategoryRegisterCommand command);

    /** 필터의 상위 카테고리가 없으면 CATEGORY_NOT_FOUND */
    List<Category> getCategories(CategorySearchCondition condition);

    Category getCategory(Long categoryId);
}

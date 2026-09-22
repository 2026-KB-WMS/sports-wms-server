package com.kb.wms.product.application.port.in;

import java.util.List;

import com.kb.wms.product.application.port.in.command.CategoryRegisterCommand;
import com.kb.wms.product.domain.entity.Category;

/**
 * 카테고리 등록/조회 유스케이스. POST, GET /api/v1/products/categories
 */
public interface CategoryUseCase {

    Category registerCategory(CategoryRegisterCommand command);

    List<Category> getCategories();

    Category getCategory(Long categoryId);
}

package com.kb.wms.product.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.CategoryUseCase;
import com.kb.wms.product.application.port.in.command.CategoryRegisterCommand;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService implements CategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public Category registerCategory(CategoryRegisterCommand command) {
        if (categoryRepository.existsByCategoryCode(command.categoryCode())) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_CATEGORY_CODE);
        }

        int depth = 1;
        if (command.parentCategoryId() != null) {
            Category parent = categoryRepository.findById(command.parentCategoryId())
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.PARENT_CATEGORY_NOT_FOUND));
            if (!parent.isActive()) {
                throw new BusinessException(ProductErrorCode.PARENT_CATEGORY_INACTIVE);
            }
            depth = parent.getDepth() + 1;
        }

        Category category = Category.register(
                command.parentCategoryId(), command.categoryCode(), command.name(), depth, command.sortOrder());
        return categoryRepository.save(category);
    }

    @Override
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));
    }
}

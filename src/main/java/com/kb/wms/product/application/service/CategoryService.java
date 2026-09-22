package com.kb.wms.product.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.CategoryUseCase;
import com.kb.wms.product.application.port.in.command.CategoryRegisterCommand;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.domain.entity.Category;

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
            throw new BusinessException(ErrorCode.CONFLICT, "이미 존재하는 카테고리 코드입니다.");
        }

        int depth = 1;
        if (command.parentCategoryId() != null) {
            Category parent = categoryRepository.findById(command.parentCategoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상위 카테고리를 찾을 수 없습니다."));
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
}

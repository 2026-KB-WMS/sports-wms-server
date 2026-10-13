package com.kb.wms.product.application.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.CategoryUseCase;
import com.kb.wms.product.application.port.in.query.CategorySearchCondition;
import com.kb.wms.product.application.port.in.command.CategoryRegisterCommand;
import com.kb.wms.product.application.port.in.command.CategoryUpdateCommand;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService implements CategoryUseCase {

    private static final Pattern CATEGORY_CODE_PATTERN = Pattern.compile("^[A-Za-z]{3}$");

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public Category registerCategory(CategoryRegisterCommand command) {
        String categoryCode = normalizeCategoryCode(command.categoryCode());
        if (categoryRepository.existsByCategoryCode(categoryCode)) {
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
                command.parentCategoryId(), categoryCode, command.name(), depth, command.sortOrder());
        return categoryRepository.save(category);
    }

    /** 영문 3자만 허용하고 대문자로 맞춘다. 웹 검증을 거치지 않은 호출도 같은 규칙을 지키게 한다. */
    private static String normalizeCategoryCode(String categoryCode) {
        if (categoryCode == null || !CATEGORY_CODE_PATTERN.matcher(categoryCode).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "카테고리 코드는 영문 3자여야 합니다.");
        }
        return categoryCode.toUpperCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public Category updateCategory(CategoryUpdateCommand command) {
        if (command.hasNoChanges()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 하나 이상 입력해주세요.");
        }
        Category category = categoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));
        if (command.name() != null) {
            category.changeName(command.name());
        }
        if (command.sortOrder() != null) {
            category.changeSortOrder(command.sortOrder());
        }
        return categoryRepository.save(category);
    }

    @Override
    public List<Category> getCategories(CategorySearchCondition condition) {
        if (condition.parentCategoryId() != null
                && categoryRepository.findById(condition.parentCategoryId()).isEmpty()) {
            throw new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND);
        }
        return categoryRepository.search(condition);
    }

    @Override
    public Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));
    }
}

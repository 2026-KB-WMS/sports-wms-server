package com.kb.wms.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.command.CategoryRegisterCommand;
import com.kb.wms.product.application.port.in.query.CategorySearchCondition;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("상위 카테고리 없이 등록하면 depth는 1이다")
    void registerCategory_root_depthIsOne() {
        CategoryRegisterCommand command = new CategoryRegisterCommand(null, "RACKET", "라켓", 1);
        when(categoryRepository.existsByCategoryCode("RACKET")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.registerCategory(command);

        assertThat(result.getDepth()).isEqualTo(1);
        assertThat(result.getCategoryCode()).isEqualTo("RACKET");
        assertThat(result.isActive()).isTrue();
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("상위 카테고리가 있으면 depth는 상위 depth + 1이다")
    void registerCategory_withParent_depthIsParentDepthPlusOne() {
        Category parent = Category.register(null, "RACKET", "라켓", 1, 0);
        CategoryRegisterCommand command = new CategoryRegisterCommand(1L, "RACKET_BEGINNER", "초보자용 라켓", 1);
        when(categoryRepository.existsByCategoryCode("RACKET_BEGINNER")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.registerCategory(command);

        assertThat(result.getDepth()).isEqualTo(2);
    }

    @Test
    @DisplayName("카테고리 코드가 중복되면 DUPLICATE_CATEGORY_CODE 예외를 던진다")
    void registerCategory_duplicateCode_throwsBusinessException() {
        CategoryRegisterCommand command = new CategoryRegisterCommand(null, "RACKET", "라켓", 1);
        when(categoryRepository.existsByCategoryCode("RACKET")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.registerCategory(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_CATEGORY_CODE.name());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 상위 카테고리를 지정하면 PARENT_CATEGORY_NOT_FOUND 예외를 던진다")
    void registerCategory_parentNotFound_throwsBusinessException() {
        CategoryRegisterCommand command = new CategoryRegisterCommand(999L, "RACKET_BEGINNER", "초보자용 라켓", 1);
        when(categoryRepository.existsByCategoryCode("RACKET_BEGINNER")).thenReturn(false);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.registerCategory(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PARENT_CATEGORY_NOT_FOUND.name());
    }

    @Test
    @DisplayName("상위 카테고리가 비활성 상태면 PARENT_CATEGORY_INACTIVE 예외를 던진다")
    void registerCategory_parentInactive_throwsBusinessException() {
        Category parent = Category.register(null, "RACKET", "라켓", 1, 0);
        parent.deactivate();
        CategoryRegisterCommand command = new CategoryRegisterCommand(1L, "RACKET_BEGINNER", "초보자용 라켓", 1);
        when(categoryRepository.existsByCategoryCode("RACKET_BEGINNER")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> categoryService.registerCategory(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PARENT_CATEGORY_INACTIVE.name());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("카테고리 목록을 검색 조건과 함께 리포지토리에 위임해 그대로 반환한다")
    void getCategories_returnsAll() {
        Category category = Category.register(null, "RACKET", "라켓", 1, 0);
        CategorySearchCondition condition = new CategorySearchCondition(null, 1, "라켓", true);
        when(categoryRepository.search(condition)).thenReturn(List.of(category));

        List<Category> result = categoryService.getCategories(condition);

        assertThat(result).containsExactly(category);
    }

    @Test
    @DisplayName("존재하지 않는 상위 카테고리로 필터링하면 CATEGORY_NOT_FOUND 예외를 던진다")
    void getCategories_parentNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategories(new CategorySearchCondition(999L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.CATEGORY_NOT_FOUND.name());
        verify(categoryRepository, never()).search(any());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리를 조회하면 CATEGORY_NOT_FOUND 예외를 던진다")
    void getCategory_notFound_throwsBusinessException() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.CATEGORY_NOT_FOUND.name());
    }
}

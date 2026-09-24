package com.kb.wms.product.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.product.adapter.in.web.dto.request.CategoryRegisterRequest;
import com.kb.wms.product.adapter.in.web.dto.response.CategoryResponse;
import com.kb.wms.product.application.port.in.CategoryUseCase;
import com.kb.wms.product.application.port.in.query.CategorySearchCondition;
import com.kb.wms.product.domain.entity.Category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 카테고리 등록/조회. POST, GET /api/v1/products/categories
 * 페이지네이션·역할별 응답 차등은 아직 적용하지 않는다(#20 1단계).
 */
@RestController
@RequestMapping("/api/v1/products/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryUseCase categoryUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> registerCategory(@Valid @RequestBody CategoryRegisterRequest request) {
        Category category = categoryUseCase.registerCategory(request.toCommand());
        return ApiResponse.created(CategoryResponse.from(category));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<CategoryResponse>> getCategories(
            @RequestParam(required = false) Long parentCategoryId,
            @RequestParam(required = false) Integer depth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive) {
        List<CategoryResponse> items = categoryUseCase
                .getCategories(new CategorySearchCondition(parentCategoryId, depth, keyword, isActive)).stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }
}

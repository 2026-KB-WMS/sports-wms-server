package com.kb.wms.product.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.product.adapter.in.web.dto.request.CategoryOptionGroupAssignRequest;
import com.kb.wms.product.adapter.in.web.dto.response.CategoryOptionGroupResponse;
import com.kb.wms.product.application.port.in.CategoryOptionGroupUseCase;
import com.kb.wms.product.application.port.in.result.CategoryOptionGroupSummary;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 카테고리별 옵션 그룹 지정(SPEC·SKU). PUT, GET /api/v1/products/categories/{categoryId}/option-groups
 */
@Tag(name = "카테고리")
@RestController
@RequiredArgsConstructor
public class CategoryOptionGroupController {

    private final CategoryOptionGroupUseCase categoryOptionGroupUseCase;

    @PutMapping("/api/v1/products/categories/{categoryId}/option-groups")
    public ApiResponse<ItemsResponse<CategoryOptionGroupResponse>> assignOptionGroups(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryOptionGroupAssignRequest request) {
        return ApiResponse.ok(toResponse(categoryId,
                categoryOptionGroupUseCase.assignOptionGroups(request.toCommand(categoryId))));
    }

    @GetMapping("/api/v1/products/categories/{categoryId}/option-groups")
    public ApiResponse<ItemsResponse<CategoryOptionGroupResponse>> getOptionGroups(@PathVariable Long categoryId) {
        return ApiResponse.ok(toResponse(categoryId, categoryOptionGroupUseCase.getOptionGroups(categoryId)));
    }

    private ItemsResponse<CategoryOptionGroupResponse> toResponse(Long categoryId,
                                                                  List<CategoryOptionGroupSummary> summaries) {
        return ItemsResponse.of(summaries.stream()
                .map(summary -> CategoryOptionGroupResponse.of(summary, categoryId))
                .toList());
    }
}

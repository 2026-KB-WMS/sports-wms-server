package com.kb.wms.product.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.product.adapter.in.web.dto.request.OptionGroupRegisterRequest;
import com.kb.wms.product.adapter.in.web.dto.response.OptionGroupResponse;
import com.kb.wms.product.adapter.in.web.dto.response.ProductOptionGroupsResponse;
import com.kb.wms.product.application.port.in.OptionGroupUseCase;
import com.kb.wms.product.domain.entity.OptionGroup;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 옵션 그룹 등록, 상품별 옵션 그룹 조회.
 * POST /api/v1/products/option-groups, GET /api/v1/products/{productId}/option-groups
 */
@RestController
@RequiredArgsConstructor
public class OptionGroupController {

    private final OptionGroupUseCase optionGroupUseCase;

    @PostMapping("/api/v1/products/option-groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OptionGroupResponse> registerOptionGroup(
            @Valid @RequestBody OptionGroupRegisterRequest request) {
        OptionGroup optionGroup = optionGroupUseCase.registerOptionGroup(request.toCommand());
        return ApiResponse.created(OptionGroupResponse.from(optionGroup));
    }

    @GetMapping("/api/v1/products/{productId}/option-groups")
    public ApiResponse<ProductOptionGroupsResponse> getOptionGroupsByProduct(@PathVariable Long productId) {
        return ApiResponse.ok(
                ProductOptionGroupsResponse.of(productId, optionGroupUseCase.getOptionGroupsByProduct(productId)));
    }
}

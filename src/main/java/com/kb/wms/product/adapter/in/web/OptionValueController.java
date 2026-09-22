package com.kb.wms.product.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.product.adapter.in.web.dto.request.OptionValueRegisterRequest;
import com.kb.wms.product.adapter.in.web.dto.response.OptionValueResponse;
import com.kb.wms.product.application.port.in.OptionValueUseCase;
import com.kb.wms.product.domain.entity.OptionValue;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 옵션 값 등록. POST /api/v1/products/option-groups/{optionGroupId}/values
 */
@RestController
@RequiredArgsConstructor
public class OptionValueController {

    private final OptionValueUseCase optionValueUseCase;

    @PostMapping("/api/v1/products/option-groups/{optionGroupId}/values")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OptionValueResponse> registerOptionValue(
            @PathVariable Long optionGroupId,
            @Valid @RequestBody OptionValueRegisterRequest request) {
        OptionValue optionValue = optionValueUseCase.registerOptionValue(request.toCommand(optionGroupId));
        return ApiResponse.created(OptionValueResponse.from(optionValue));
    }
}

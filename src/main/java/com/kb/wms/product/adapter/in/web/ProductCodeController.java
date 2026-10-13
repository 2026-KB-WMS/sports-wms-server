package com.kb.wms.product.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.product.adapter.in.web.dto.response.CodeItemResponse;
import com.kb.wms.product.application.port.in.ProductCodeUseCase;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 상품 도메인 고정 코드 목록 조회.
 * GET /api/v1/products/skus/units
 */
@Tag(name = "상품 코드")
@RestController
@RequestMapping("/api/v1/products/skus")
@RequiredArgsConstructor
public class ProductCodeController {

    private final ProductCodeUseCase productCodeUseCase;

    @GetMapping("/units")
    public ApiResponse<ItemsResponse<CodeItemResponse>> getSkuUnits() {
        List<CodeItemResponse> items = productCodeUseCase.getSkuUnits().stream()
                .map(CodeItemResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }
}

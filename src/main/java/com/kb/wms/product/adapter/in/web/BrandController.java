package com.kb.wms.product.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.product.adapter.in.web.dto.response.BrandResponse;
import com.kb.wms.product.application.port.in.BrandQueryUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 브랜드 조회. GET /api/v1/products/brands
 * 브랜드 등록·수정 API는 명세에 없어 이 컨트롤러에는 조회만 둔다.
 */
@RestController
@RequestMapping("/api/v1/products/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandQueryUseCase brandQueryUseCase;

    @GetMapping
    public ApiResponse<List<BrandResponse>> getBrands() {
        List<BrandResponse> items = brandQueryUseCase.getBrands().stream()
                .map(BrandResponse::from)
                .toList();
        return ApiResponse.ok(items);
    }
}

package com.kb.wms.product.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.product.adapter.in.web.dto.request.BrandRegisterRequest;
import com.kb.wms.product.adapter.in.web.dto.response.BrandCreateResponse;
import com.kb.wms.product.adapter.in.web.dto.response.BrandResponse;
import com.kb.wms.product.application.port.in.BrandQueryUseCase;
import com.kb.wms.product.domain.entity.Brand;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 브랜드 등록/조회. POST, GET /api/v1/products/brands
 */
@RestController
@RequestMapping("/api/v1/products/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandQueryUseCase brandQueryUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BrandCreateResponse> registerBrand(@Valid @RequestBody BrandRegisterRequest request) {
        Brand brand = brandQueryUseCase.registerBrand(request.toCommand());
        return ApiResponse.created(BrandCreateResponse.from(brand));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<BrandResponse>> getBrands() {
        List<BrandResponse> items = brandQueryUseCase.getBrands().stream()
                .map(BrandResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }
}

package com.kb.wms.product.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.product.adapter.in.web.dto.request.ProductRegisterRequest;
import com.kb.wms.product.adapter.in.web.dto.request.ProductUpdateRequest;
import com.kb.wms.product.adapter.in.web.dto.response.ProductDetailResponse;
import com.kb.wms.product.adapter.in.web.dto.response.ProductSummaryResponse;
import com.kb.wms.product.application.port.in.BrandQueryUseCase;
import com.kb.wms.product.application.port.in.CategoryUseCase;
import com.kb.wms.product.application.port.in.ProductUseCase;
import com.kb.wms.product.domain.entity.Product;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 상품 등록/조회.
 * POST /api/v1/products, GET /api/v1/products, GET /api/v1/products/{productId}
 * 페이지네이션·역할별 응답 차등은 아직 적용하지 않는다(#20 1단계).
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductUseCase productUseCase;
    private final BrandQueryUseCase brandQueryUseCase;
    private final CategoryUseCase categoryUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductDetailResponse> registerProduct(@Valid @RequestBody ProductRegisterRequest request) {
        Product product = productUseCase.registerProduct(request.toCommand());
        return ApiResponse.created(toDetailResponse(product));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<ProductSummaryResponse>> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId) {
        List<ProductSummaryResponse> items = productUseCase.getProducts(brandId, categoryId).stream()
                .map(this::toSummaryResponse)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> getProduct(@PathVariable Long productId) {
        Product product = productUseCase.getProduct(productId);
        return ApiResponse.ok(toDetailResponse(product));
    }

    @PatchMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request) {
        Product product = productUseCase.updateProduct(request.toCommand(productId));
        return ApiResponse.ok(toDetailResponse(product));
    }

    private ProductSummaryResponse toSummaryResponse(Product product) {
        return ProductSummaryResponse.of(product, brandName(product.getBrandId()), categoryName(product.getCategoryId()));
    }

    private ProductDetailResponse toDetailResponse(Product product) {
        return ProductDetailResponse.of(product, brandName(product.getBrandId()), categoryName(product.getCategoryId()));
    }

    private String brandName(Long brandId) {
        return brandQueryUseCase.getBrand(brandId).getName();
    }

    private String categoryName(Long categoryId) {
        return categoryUseCase.getCategory(categoryId).getName();
    }
}

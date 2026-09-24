package com.kb.wms.product.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.product.adapter.in.web.dto.request.ProductSkuRegisterRequest;
import com.kb.wms.product.adapter.in.web.dto.request.SkuOptionConnectRequest;
import com.kb.wms.product.adapter.in.web.dto.response.ProductSkuCreateResponse;
import com.kb.wms.product.adapter.in.web.dto.response.ProductSkuDetailResponse;
import com.kb.wms.product.adapter.in.web.dto.response.ProductSkuListItemResponse;
import com.kb.wms.product.adapter.in.web.dto.response.SkuOptionsConnectResponse;
import com.kb.wms.product.application.port.in.BrandQueryUseCase;
import com.kb.wms.product.application.port.in.CategoryUseCase;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;
import com.kb.wms.product.application.port.in.ProductUseCase;
import com.kb.wms.product.application.port.in.query.ProductSkuSearchCondition;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.domain.entity.ProductSku;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SKU 등록/조회/옵션 연결.
 * POST/GET /api/v1/products/skus, GET /api/v1/products/skus/{skuId}, POST /api/v1/products/skus/{skuId}/options
 * 페이지네이션·역할별 응답 차등은 아직 적용하지 않는다(#20 1단계).
 */
@RestController
@RequestMapping("/api/v1/products/skus")
@RequiredArgsConstructor
public class ProductSkuController {

    private final ProductSkuUseCase productSkuUseCase;
    private final ProductUseCase productUseCase;
    private final BrandQueryUseCase brandQueryUseCase;
    private final CategoryUseCase categoryUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductSkuCreateResponse> registerSku(@Valid @RequestBody ProductSkuRegisterRequest request) {
        ProductSku sku = productSkuUseCase.registerSku(request.toCommand());
        return ApiResponse.created(ProductSkuCreateResponse.from(sku));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<ProductSkuListItemResponse>> getSkus(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive) {
        List<ProductSkuListItemResponse> items = productSkuUseCase
                .getSkus(new ProductSkuSearchCondition(productId, brandId, categoryId, keyword, isActive)).stream()
                .map(sku -> ProductSkuListItemResponse.of(
                        sku,
                        productUseCase.getProduct(sku.getProductId()).getName(),
                        productSkuUseCase.getSkuOptions(sku.getSkuId())))
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{skuId}")
    public ApiResponse<ProductSkuDetailResponse> getSku(@PathVariable Long skuId) {
        ProductSku sku = productSkuUseCase.getSku(skuId);
        Product product = productUseCase.getProduct(sku.getProductId());
        ProductSkuDetailResponse response = ProductSkuDetailResponse.of(
                sku,
                product.getProductCode(),
                product.getName(),
                product.getBrandId(),
                brandQueryUseCase.getBrand(product.getBrandId()).getName(),
                product.getCategoryId(),
                categoryUseCase.getCategory(product.getCategoryId()).getName(),
                productSkuUseCase.getSkuOptions(sku.getSkuId()));
        return ApiResponse.ok(response);
    }

    @PostMapping("/{skuId}/options")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkuOptionsConnectResponse> connectOptions(
            @PathVariable Long skuId,
            @Valid @RequestBody SkuOptionConnectRequest request) {
        productSkuUseCase.connectOptions(request.toCommand(skuId));
        SkuOptionsConnectResponse response =
                SkuOptionsConnectResponse.of(skuId, productSkuUseCase.getSkuOptions(skuId));
        return ApiResponse.created(response);
    }
}

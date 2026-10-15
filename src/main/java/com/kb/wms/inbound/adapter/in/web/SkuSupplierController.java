package com.kb.wms.inbound.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.inbound.adapter.in.web.dto.request.SkuSupplierRegisterRequest;
import com.kb.wms.inbound.adapter.in.web.dto.response.SkuSupplierRemoveResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.SkuSupplierResponse;
import com.kb.wms.inbound.application.port.in.SkuSupplierUseCase;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SKU↔공급처 매핑 관리. GET, POST /api/v1/products/skus/{skuId}/suppliers, DELETE .../{supplierId}
 * 조회는 본사+창고 관리자, 등록·삭제는 본사만 가능하다(SecurityConfig). 점주에게는 공급처 정보를 열지 않는다.
 */
@Tag(name = "SKU 공급처 매핑")
@RestController
@RequestMapping("/api/v1/products/skus/{skuId}/suppliers")
@RequiredArgsConstructor
public class SkuSupplierController {

    private final SkuSupplierUseCase skuSupplierUseCase;

    @GetMapping
    public ApiResponse<ItemsResponse<SkuSupplierResponse>> getSuppliers(@PathVariable Long skuId) {
        List<SkuSupplierResponse> items = skuSupplierUseCase.getSuppliers(skuId).stream()
                .map(SkuSupplierResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkuSupplierResponse> registerSupplier(@PathVariable Long skuId,
                                                             @Valid @RequestBody SkuSupplierRegisterRequest request) {
        return ApiResponse.created(SkuSupplierResponse.from(
                skuSupplierUseCase.registerSupplier(skuId, request.supplierId())));
    }

    @DeleteMapping("/{supplierId}")
    public ApiResponse<SkuSupplierRemoveResponse> removeSupplier(@PathVariable Long skuId,
                                                                 @PathVariable Long supplierId) {
        skuSupplierUseCase.removeSupplier(skuId, supplierId);
        return ApiResponse.ok(new SkuSupplierRemoveResponse(skuId, supplierId));
    }
}

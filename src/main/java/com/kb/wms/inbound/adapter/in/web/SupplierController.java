package com.kb.wms.inbound.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.inbound.adapter.in.web.dto.request.SupplierRegisterRequest;
import com.kb.wms.inbound.adapter.in.web.dto.request.SupplierUpdateRequest;
import com.kb.wms.inbound.adapter.in.web.dto.response.SupplierDeactivateResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.SupplierResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.SupplierSummaryResponse;
import com.kb.wms.inbound.application.port.in.SupplierUseCase;
import com.kb.wms.inbound.application.port.in.query.SupplierSearchCondition;
import com.kb.wms.inbound.domain.entity.Supplier;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 공급처 등록/조회/수정/비활성화.
 * POST, GET, PATCH /api/v1/suppliers
 * 인증/인가가 아직 구현되지 않아 역할별 규칙(창고 관리자는 활성 공급처만 조회)은 적용하지 않는다.
 * 인증 연동 시 이 컨트롤러에서 역할을 보고 isActive=true 강제와 비활성 공급처 상세 404 처리를 추가한다.
 */
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierUseCase supplierUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupplierResponse> registerSupplier(@Valid @RequestBody SupplierRegisterRequest request) {
        Supplier supplier = supplierUseCase.registerSupplier(request.toCommand());
        return ApiResponse.created(SupplierResponse.from(supplier));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<SupplierSummaryResponse>> getSuppliers(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive) {
        List<SupplierSummaryResponse> items = supplierUseCase
                .getSuppliers(new SupplierSearchCondition(keyword, isActive), principal).stream()
                .map(SupplierSummaryResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{supplierId}")
    public ApiResponse<SupplierResponse> getSupplier(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @PathVariable Long supplierId) {
        Supplier supplier = supplierUseCase.getSupplier(supplierId, principal);
        return ApiResponse.ok(SupplierResponse.from(supplier));
    }

    @PatchMapping("/{supplierId}")
    public ApiResponse<SupplierResponse> updateSupplier(
            @PathVariable Long supplierId,
            @Valid @RequestBody SupplierUpdateRequest request) {
        Supplier supplier = supplierUseCase.updateSupplier(supplierId, request.toCommand());
        return ApiResponse.ok(SupplierResponse.from(supplier));
    }

    @PatchMapping("/{supplierId}/deactivate")
    public ApiResponse<SupplierDeactivateResponse> deactivateSupplier(@PathVariable Long supplierId) {
        Supplier supplier = supplierUseCase.deactivateSupplier(supplierId);
        return ApiResponse.ok(SupplierDeactivateResponse.from(supplier));
    }
}

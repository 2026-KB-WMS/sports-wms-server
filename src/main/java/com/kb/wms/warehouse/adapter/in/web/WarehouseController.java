package com.kb.wms.warehouse.adapter.in.web;

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

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.warehouse.adapter.in.web.dto.request.WarehouseRegisterRequest;
import com.kb.wms.warehouse.adapter.in.web.dto.request.WarehouseUpdateRequest;
import com.kb.wms.warehouse.adapter.in.web.dto.response.WarehouseMembershipResponse;
import com.kb.wms.warehouse.adapter.in.web.dto.response.WarehouseResponse;
import com.kb.wms.warehouse.adapter.in.web.dto.response.WarehouseSummaryResponse;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.query.WarehouseSearchCondition;
import com.kb.wms.warehouse.domain.entity.Warehouse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 창고 등록/조회/수정/비활성화.
 * POST, GET, PATCH /api/v1/warehouses, GET /api/v1/warehouses/my
 * my 조회는 토큰 주체 본인의 소속 창고를 돌려주고, 단건 조회는 서비스가 담당 창고(HQ_ADMIN은 전체)만 허용한다.
 */
@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseUseCase warehouseUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WarehouseResponse> registerWarehouse(@Valid @RequestBody WarehouseRegisterRequest request) {
        Warehouse warehouse = warehouseUseCase.registerWarehouse(request.toCommand());
        return ApiResponse.created(WarehouseResponse.from(warehouse));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<WarehouseSummaryResponse>> getWarehouses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive) {
        List<WarehouseSummaryResponse> items = warehouseUseCase
                .getWarehouses(new WarehouseSearchCondition(keyword, isActive)).stream()
                .map(WarehouseSummaryResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/my")
    public ApiResponse<ItemsResponse<WarehouseMembershipResponse>> getMyWarehouses(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        List<WarehouseMembershipResponse> items = warehouseUseCase.getMyWarehouses(principal.userId()).stream()
                .map(WarehouseMembershipResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{warehouseId}")
    public ApiResponse<WarehouseResponse> getWarehouse(@AuthenticationPrincipal AuthenticatedUser principal,
                                                       @PathVariable Long warehouseId) {
        Warehouse warehouse = warehouseUseCase.getWarehouse(warehouseId, principal);
        return ApiResponse.ok(WarehouseResponse.from(warehouse));
    }

    @PatchMapping("/{warehouseId}")
    public ApiResponse<WarehouseResponse> updateWarehouse(
            @PathVariable Long warehouseId,
            @Valid @RequestBody WarehouseUpdateRequest request) {
        Warehouse warehouse = warehouseUseCase.updateWarehouse(warehouseId, request.toCommand());
        return ApiResponse.ok(WarehouseResponse.from(warehouse));
    }

    @PatchMapping("/{warehouseId}/deactivate")
    public ApiResponse<WarehouseResponse> deactivateWarehouse(@PathVariable Long warehouseId) {
        Warehouse warehouse = warehouseUseCase.deactivateWarehouse(warehouseId);
        return ApiResponse.ok(WarehouseResponse.from(warehouse));
    }
}

package com.kb.wms.warehouse.adapter.in.web;

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
import com.kb.wms.warehouse.adapter.in.web.dto.request.WarehouseRegisterRequest;
import com.kb.wms.warehouse.adapter.in.web.dto.request.WarehouseUpdateRequest;
import com.kb.wms.warehouse.adapter.in.web.dto.response.WarehouseMembershipResponse;
import com.kb.wms.warehouse.adapter.in.web.dto.response.WarehouseResponse;
import com.kb.wms.warehouse.adapter.in.web.dto.response.WarehouseSummaryResponse;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.domain.entity.Warehouse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 창고 등록/조회/수정/비활성화.
 * POST, GET, PATCH /api/v1/warehouses, GET /api/v1/warehouses/my
 * 인증/인가가 아직 구현되지 않아 my 조회는 userId를 쿼리 파라미터로 받는다(추후 인증 연동 시 교체 예정).
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
    public ApiResponse<List<WarehouseSummaryResponse>> getWarehouses() {
        List<WarehouseSummaryResponse> items = warehouseUseCase.getWarehouses().stream()
                .map(WarehouseSummaryResponse::from)
                .toList();
        return ApiResponse.ok(items);
    }

    @GetMapping("/my")
    public ApiResponse<List<WarehouseMembershipResponse>> getMyWarehouses(@RequestParam Long userId) {
        List<WarehouseMembershipResponse> items = warehouseUseCase.getMyWarehouses(userId).stream()
                .map(WarehouseMembershipResponse::from)
                .toList();
        return ApiResponse.ok(items);
    }

    @GetMapping("/{warehouseId}")
    public ApiResponse<WarehouseResponse> getWarehouse(@PathVariable Long warehouseId) {
        Warehouse warehouse = warehouseUseCase.getWarehouse(warehouseId);
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

package com.kb.wms.warehouse.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.kb.wms.warehouse.adapter.in.web.dto.request.WarehouseMemberAssignRequest;
import com.kb.wms.warehouse.adapter.in.web.dto.response.WarehouseMemberReleaseResponse;
import com.kb.wms.warehouse.adapter.in.web.dto.response.WarehouseMemberResponse;
import com.kb.wms.warehouse.application.port.in.WarehouseMemberUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.domain.entity.WarehouseMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 창고 관리자 배정/조회/배정해제.
 * POST, GET /api/v1/warehouses/managers, DELETE /api/v1/warehouses/managers/{warehouseMemberId}
 */
@RestController
@RequestMapping("/api/v1/warehouses/managers")
@RequiredArgsConstructor
public class WarehouseMemberController {

    private final WarehouseMemberUseCase warehouseMemberUseCase;
    private final WarehouseUseCase warehouseUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WarehouseMemberResponse> assignManager(
            @Valid @RequestBody WarehouseMemberAssignRequest request) {
        WarehouseMember member = warehouseMemberUseCase.assignManager(request.toCommand());
        return ApiResponse.created(toResponse(member));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<WarehouseMemberResponse>> getManagers(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long userId) {
        List<WarehouseMemberResponse> items = warehouseMemberUseCase.getManagers(warehouseId, userId).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @DeleteMapping("/{warehouseMemberId}")
    public ApiResponse<WarehouseMemberReleaseResponse> releaseManager(@PathVariable Long warehouseMemberId) {
        warehouseMemberUseCase.releaseManager(warehouseMemberId);
        return ApiResponse.ok(new WarehouseMemberReleaseResponse(warehouseMemberId));
    }

    private WarehouseMemberResponse toResponse(WarehouseMember member) {
        String warehouseName = warehouseUseCase.getWarehouse(member.getWarehouseId()).getName();
        return WarehouseMemberResponse.of(member, warehouseName);
    }
}

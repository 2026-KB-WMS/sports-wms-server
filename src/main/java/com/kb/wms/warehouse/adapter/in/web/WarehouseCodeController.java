package com.kb.wms.warehouse.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.warehouse.adapter.in.web.dto.response.CodeItemResponse;
import com.kb.wms.warehouse.application.port.in.WarehouseCodeUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 창고 도메인 고정 코드 목록 조회.
 * GET /api/v1/warehouses/management-types, GET /api/v1/warehouses/section-types
 */
@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseCodeController {

    private final WarehouseCodeUseCase warehouseCodeUseCase;

    @GetMapping("/management-types")
    public ApiResponse<ItemsResponse<CodeItemResponse>> getManagementTypes() {
        List<CodeItemResponse> items = warehouseCodeUseCase.getManagementTypes().stream()
                .map(CodeItemResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/section-types")
    public ApiResponse<ItemsResponse<CodeItemResponse>> getSectionTypes() {
        List<CodeItemResponse> items = warehouseCodeUseCase.getSectionTypes().stream()
                .map(CodeItemResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }
}

package com.kb.wms.warehouse.adapter.in.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.warehouse.adapter.in.web.dto.request.WarehouseSectionRegisterRequest;
import com.kb.wms.warehouse.adapter.in.web.dto.request.WarehouseSectionUpdateRequest;
import com.kb.wms.warehouse.adapter.in.web.dto.response.WarehouseSectionResponse;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.query.WarehouseSectionSearchCondition;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 창고 구역 등록/조회/수정/비활성화.
 * POST /api/v1/warehouses/sections, GET /api/v1/warehouses/sections,
 * GET /api/v1/warehouses/{warehouseId}/sections, PATCH /api/v1/warehouses/sections/{sectionId}(/deactivate)
 * 서로 다른 두 기준 경로(/warehouses/sections, /warehouses/{warehouseId}/sections)를 함께 다루기 위해
 * 클래스 레벨 @RequestMapping 없이 메서드마다 전체 경로를 명시한다.
 */
@RestController
@RequiredArgsConstructor
public class WarehouseSectionController {

    private final WarehouseSectionUseCase warehouseSectionUseCase;
    private final WarehouseUseCase warehouseUseCase;

    @PostMapping("/api/v1/warehouses/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WarehouseSectionResponse> registerSection(
            @Valid @RequestBody WarehouseSectionRegisterRequest request) {
        WarehouseSection section = warehouseSectionUseCase.registerSection(request.toCommand());
        return ApiResponse.created(toResponse(section));
    }

    @GetMapping("/api/v1/warehouses/sections")
    public ApiResponse<ItemsResponse<WarehouseSectionResponse>> getAllSections(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long parentSectionId,
            @RequestParam(required = false) String sectionType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive) {
        List<WarehouseSectionResponse> items = warehouseSectionUseCase
                .getSections(new WarehouseSectionSearchCondition(
                        warehouseId, parentSectionId, sectionType, keyword, isActive)).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/api/v1/warehouses/{warehouseId}/sections")
    public ApiResponse<ItemsResponse<WarehouseSectionResponse>> getSectionsByWarehouse(
            @PathVariable Long warehouseId,
            @RequestParam(required = false) Long parentSectionId,
            @RequestParam(required = false) String sectionType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive) {
        List<WarehouseSectionResponse> items = warehouseSectionUseCase
                .getSections(new WarehouseSectionSearchCondition(
                        warehouseId, parentSectionId, sectionType, keyword, isActive)).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/api/v1/warehouses/sections/{sectionId}")
    public ApiResponse<WarehouseSectionResponse> getSection(@PathVariable Long sectionId) {
        WarehouseSection section = warehouseSectionUseCase.getSection(sectionId);
        return ApiResponse.ok(toResponse(section));
    }

    @PatchMapping("/api/v1/warehouses/sections/{sectionId}")
    public ApiResponse<WarehouseSectionResponse> updateSection(
            @PathVariable Long sectionId,
            @Valid @RequestBody WarehouseSectionUpdateRequest request) {
        WarehouseSection section = warehouseSectionUseCase.updateSection(sectionId, request.toCommand());
        return ApiResponse.ok(toResponse(section));
    }

    @PatchMapping("/api/v1/warehouses/sections/{sectionId}/deactivate")
    public ApiResponse<WarehouseSectionResponse> deactivateSection(@PathVariable Long sectionId) {
        WarehouseSection section = warehouseSectionUseCase.deactivateSection(sectionId);
        return ApiResponse.ok(toResponse(section));
    }

    /** 목록에서는 창고명·상위 구역 코드를 창고·상위 구역당 한 번만 조회한다. */
    private List<WarehouseSectionResponse> toResponses(List<WarehouseSection> sections) {
        Map<Long, String> warehouseNames = new HashMap<>();
        Map<Long, String> parentCodes = new HashMap<>();
        return sections.stream()
                .map(section -> WarehouseSectionResponse.from(
                        section,
                        warehouseNames.computeIfAbsent(section.getWarehouseId(),
                                id -> warehouseUseCase.getWarehouse(id).getName()),
                        section.getParentSectionId() == null
                                ? null
                                : parentCodes.computeIfAbsent(section.getParentSectionId(),
                                        id -> warehouseSectionUseCase.getSection(id).getSectionCode())))
                .toList();
    }

    private WarehouseSectionResponse toResponse(WarehouseSection section) {
        String warehouseName = warehouseUseCase.getWarehouse(section.getWarehouseId()).getName();
        String parentSectionCode = section.getParentSectionId() == null
                ? null
                : warehouseSectionUseCase.getSection(section.getParentSectionId()).getSectionCode();
        return WarehouseSectionResponse.from(section, warehouseName, parentSectionCode);
    }
}

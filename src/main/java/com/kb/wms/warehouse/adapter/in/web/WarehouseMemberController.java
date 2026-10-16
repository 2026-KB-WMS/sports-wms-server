package com.kb.wms.warehouse.adapter.in.web;

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

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.SortSpec;
import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.PageResponse;
import com.kb.wms.warehouse.adapter.in.web.dto.request.WarehouseMemberAssignRequest;
import com.kb.wms.warehouse.adapter.in.web.dto.response.WarehouseMemberReleaseResponse;
import com.kb.wms.warehouse.adapter.in.web.dto.response.WarehouseMemberResponse;
import com.kb.wms.warehouse.application.port.in.WarehouseMemberUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMemberView;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 창고 관리자 배정/조회/배정해제.
 * POST, GET /api/v1/warehouses/managers, DELETE /api/v1/warehouses/managers/{warehouseMemberId}
 */
@Tag(name = "창고 관리자")
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
        WarehouseMemberView member = warehouseMemberUseCase.assignManager(request.toCommand());
        return ApiResponse.created(toResponse(member));
    }

    /** 목록 정렬 규칙. 기본은 배정 일시 최신순. */
    static final SortSpec LIST_SORT = SortSpec.builder()
            .field("assignedAt", "m.assignedAt")
            .field("userName", "u.name")
            .field("loginId", "u.loginId")
            .field("memberRole", "m.memberRole")
            .defaultOrder("assignedAt", SortDirection.DESC)
            .tieBreaker("m.warehouseMemberId")
            .build();

    @GetMapping
    public ApiResponse<PageResponse<WarehouseMemberResponse>> getManagers(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageCondition pageCondition = PageCondition.of(page, size, sort, LIST_SORT);
        return ApiResponse.ok(PageResponse.of(
                warehouseMemberUseCase.getManagers(warehouseId, userId, keyword, pageCondition),
                this::toResponse));
    }

    @DeleteMapping("/{warehouseMemberId}")
    public ApiResponse<WarehouseMemberReleaseResponse> releaseManager(@PathVariable Long warehouseMemberId) {
        warehouseMemberUseCase.releaseManager(warehouseMemberId);
        return ApiResponse.ok(new WarehouseMemberReleaseResponse(warehouseMemberId));
    }

    private WarehouseMemberResponse toResponse(WarehouseMemberView member) {
        String warehouseName = warehouseUseCase.getWarehouse(member.warehouseId()).getName();
        return WarehouseMemberResponse.of(member, warehouseName);
    }
}

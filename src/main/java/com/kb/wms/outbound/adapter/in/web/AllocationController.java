package com.kb.wms.outbound.adapter.in.web;

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
import com.kb.wms.outbound.adapter.in.web.dto.request.OutboundReasonRequest;
import com.kb.wms.outbound.adapter.in.web.dto.request.StockAllocateRequest;
import com.kb.wms.outbound.adapter.in.web.dto.response.StockAllocateResponse;
import com.kb.wms.outbound.adapter.in.web.dto.response.StockAllocationDetailResponse;
import com.kb.wms.outbound.adapter.in.web.dto.response.StockAllocationItemResponse;
import com.kb.wms.outbound.adapter.in.web.dto.response.StockAllocationReleaseResponse;
import com.kb.wms.outbound.application.port.in.StockAllocationUseCase;
import com.kb.wms.outbound.application.port.in.command.StockAllocationReleaseCommand;
import com.kb.wms.outbound.application.port.in.query.StockAllocationSearchCondition;
import com.kb.wms.outbound.domain.enums.AllocationStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 재고 할당 생성/목록/상세/해제.
 * POST, GET /api/v1/allocations, GET .../{allocationId}, PATCH .../{allocationId}/release
 *
 * <p>처리 사용자는 토큰 주체이고, 역할(생성·해제는 창고 관리자, 조회는 본사와 창고 관리자)은 SecurityConfig가,
 * 담당 창고 범위(발주에 배정된 창고)는 서비스가 검사한다. 목록은 페이지네이션 없이 전체를 반환한다.
 */
@RestController
@RequestMapping("/api/v1/allocations")
@RequiredArgsConstructor
public class AllocationController {

    private final StockAllocationUseCase stockAllocationUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StockAllocateResponse> allocate(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody StockAllocateRequest request) {
        return ApiResponse.created(StockAllocateResponse.from(stockAllocationUseCase.allocate(
                request.toCommand(principal.userId()), principal)));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<StockAllocationItemResponse>> getAllocations(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long storeOrderId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) AllocationStatus status,
            @RequestParam(required = false) String keyword) {
        List<StockAllocationItemResponse> items = stockAllocationUseCase
                .searchAllocations(StockAllocationSearchCondition.unscoped(
                        storeOrderId, warehouseId, skuId, status, keyword), principal)
                .stream()
                .map(StockAllocationItemResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{allocationId}")
    public ApiResponse<StockAllocationDetailResponse> getAllocation(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                @PathVariable Long allocationId) {
        return ApiResponse.ok(StockAllocationDetailResponse.from(
                stockAllocationUseCase.getAllocation(allocationId, principal)));
    }

    @PatchMapping("/{allocationId}/release")
    public ApiResponse<StockAllocationReleaseResponse> releaseAllocation(
            @PathVariable Long allocationId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody OutboundReasonRequest request) {
        return ApiResponse.ok(StockAllocationReleaseResponse.from(stockAllocationUseCase.release(
                new StockAllocationReleaseCommand(allocationId, request.reason(), principal.userId()), principal)));
    }
}

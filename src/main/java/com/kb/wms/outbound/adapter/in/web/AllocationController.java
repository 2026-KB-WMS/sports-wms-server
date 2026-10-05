package com.kb.wms.outbound.adapter.in.web;

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
 * <p>인증/인가가 아직 구현되지 않아 역할별 규칙(생성·해제는 담당 창고 관리자, 조회는 본사 관리자와 담당 창고 관리자,
 * 데이터 범위 제한)은 적용하지 않는다. 처리 사용자(userId)는 쿼리 파라미터로 받으며, 인증 연동 시 토큰의 사용자로
 * 대체하고 이 컨트롤러에서 역할 검사를 추가한다. 목록은 페이지네이션 없이 전체를 반환한다.
 */
@RestController
@RequestMapping("/api/v1/allocations")
@RequiredArgsConstructor
public class AllocationController {

    private final StockAllocationUseCase stockAllocationUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StockAllocateResponse> allocate(
            @RequestParam Long userId,
            @Valid @RequestBody StockAllocateRequest request) {
        return ApiResponse.created(StockAllocateResponse.from(stockAllocationUseCase.allocate(
                request.toCommand(userId))));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<StockAllocationItemResponse>> getAllocations(
            @RequestParam(required = false) Long storeOrderId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) AllocationStatus status,
            @RequestParam(required = false) String keyword) {
        List<StockAllocationItemResponse> items = stockAllocationUseCase
                .searchAllocations(new StockAllocationSearchCondition(storeOrderId, warehouseId, skuId, status, keyword))
                .stream()
                .map(StockAllocationItemResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{allocationId}")
    public ApiResponse<StockAllocationDetailResponse> getAllocation(@PathVariable Long allocationId) {
        return ApiResponse.ok(StockAllocationDetailResponse.from(stockAllocationUseCase.getAllocation(allocationId)));
    }

    @PatchMapping("/{allocationId}/release")
    public ApiResponse<StockAllocationReleaseResponse> releaseAllocation(
            @PathVariable Long allocationId,
            @RequestParam Long userId,
            @Valid @RequestBody OutboundReasonRequest request) {
        return ApiResponse.ok(StockAllocationReleaseResponse.from(stockAllocationUseCase.release(
                new StockAllocationReleaseCommand(allocationId, request.reason(), userId))));
    }
}

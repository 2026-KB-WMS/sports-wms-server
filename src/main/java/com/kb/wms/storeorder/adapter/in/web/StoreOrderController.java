package com.kb.wms.storeorder.adapter.in.web;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import com.kb.wms.storeorder.adapter.in.web.dto.request.StoreOrderAssignRequest;
import com.kb.wms.storeorder.adapter.in.web.dto.request.StoreOrderCancelRequest;
import com.kb.wms.storeorder.adapter.in.web.dto.request.StoreOrderReasonRequest;
import com.kb.wms.storeorder.adapter.in.web.dto.request.StoreOrderRegisterRequest;
import com.kb.wms.storeorder.adapter.in.web.dto.response.StoreOrderApproveResponse;
import com.kb.wms.storeorder.adapter.in.web.dto.response.StoreOrderAssignResponse;
import com.kb.wms.storeorder.adapter.in.web.dto.response.StoreOrderCancelResponse;
import com.kb.wms.storeorder.adapter.in.web.dto.response.StoreOrderCompletePartialResponse;
import com.kb.wms.storeorder.adapter.in.web.dto.response.StoreOrderDetailsResponse;
import com.kb.wms.storeorder.adapter.in.web.dto.response.StoreOrderListItemResponse;
import com.kb.wms.storeorder.adapter.in.web.dto.response.StoreOrderRegisterResponse;
import com.kb.wms.storeorder.adapter.in.web.dto.response.StoreOrderResponse;
import com.kb.wms.storeorder.adapter.in.web.dto.response.StoreOrderStatusResponse;
import com.kb.wms.storeorder.application.port.in.StoreOrderUseCase;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCancelCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderHoldCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRejectCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderResumeCommand;
import com.kb.wms.storeorder.application.port.in.query.StoreOrderSearchCondition;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 지점 발주 등록/조회/승인/반려/취소/배정/보류/재개/부분 출고 종결.
 * POST, GET /api/v1/orders, GET .../{orderId}, GET .../{orderId}/details,
 * PATCH .../{orderId}/approve, reject, cancel, hold, resume, complete-partial, POST .../assign
 *
 * <p>처리 사용자는 토큰 주체이고, 역할은 SecurityConfig가, 담당 지점·창고 범위와 취소 권한(작성자·상태별)은 서비스가 검사한다.
 * {@code GET /orders/my}(소속 지점·창고 범위 조회)는 아직 구현하지 않았다.
 * 목록은 페이지네이션 없이 전체를 반환한다(공통 페이징 도입 시 추가).
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class StoreOrderController {

    private final StoreOrderUseCase storeOrderUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StoreOrderRegisterResponse> registerStoreOrder(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody StoreOrderRegisterRequest request) {
        Long storeOrderId = storeOrderUseCase.registerStoreOrder(request.toCommand(principal.userId()), principal);
        return ApiResponse.created(StoreOrderRegisterResponse.of(
                storeOrderUseCase.getStoreOrder(storeOrderId, principal).view(),
                storeOrderUseCase.getStoreOrderDetails(storeOrderId, principal).items()));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<StoreOrderListItemResponse>> getStoreOrders(
            @RequestParam(required = false) StoreOrderStatus status,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime requestedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime requestedTo) {
        List<StoreOrderListItemResponse> items = storeOrderUseCase
                .getStoreOrders(new StoreOrderSearchCondition(
                        status, storeId, warehouseId, keyword, requestedFrom, requestedTo))
                .stream()
                .map(StoreOrderListItemResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<StoreOrderResponse> getStoreOrder(@AuthenticationPrincipal AuthenticatedUser principal,
                                                      @PathVariable Long orderId) {
        return ApiResponse.ok(StoreOrderResponse.from(storeOrderUseCase.getStoreOrder(orderId, principal)));
    }

    @GetMapping("/{orderId}/details")
    public ApiResponse<StoreOrderDetailsResponse> getStoreOrderDetails(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long orderId) {
        return ApiResponse.ok(StoreOrderDetailsResponse.from(
                storeOrderUseCase.getStoreOrderDetails(orderId, principal)));
    }

    @PatchMapping("/{orderId}/approve")
    public ApiResponse<StoreOrderApproveResponse> approveStoreOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(StoreOrderApproveResponse.from(storeOrderUseCase.approveStoreOrder(orderId, principal.userId())));
    }

    @PatchMapping("/{orderId}/reject")
    public ApiResponse<StoreOrderStatusResponse> rejectStoreOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody StoreOrderReasonRequest request) {
        return ApiResponse.ok(StoreOrderStatusResponse.from(storeOrderUseCase.rejectStoreOrder(
                new StoreOrderRejectCommand(orderId, request.reason(), principal.userId()))));
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<StoreOrderCancelResponse> cancelStoreOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody(required = false) StoreOrderCancelRequest request) {
        String reason = request != null ? request.reason() : null;
        return ApiResponse.ok(StoreOrderCancelResponse.from(storeOrderUseCase.cancelStoreOrder(
                new StoreOrderCancelCommand(orderId, reason, principal.userId()), principal)));
    }

    @PostMapping("/assign")
    public ApiResponse<StoreOrderAssignResponse> assignStoreOrder(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody StoreOrderAssignRequest request) {
        return ApiResponse.ok(StoreOrderAssignResponse.from(
                storeOrderUseCase.assignStoreOrder(request.toCommand(principal.userId()))));
    }

    @PatchMapping("/{orderId}/hold")
    public ApiResponse<StoreOrderStatusResponse> holdStoreOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody StoreOrderReasonRequest request) {
        return ApiResponse.ok(StoreOrderStatusResponse.from(storeOrderUseCase.holdStoreOrder(
                new StoreOrderHoldCommand(orderId, request.reason(), principal.userId()), principal)));
    }

    @PatchMapping("/{orderId}/resume")
    public ApiResponse<StoreOrderStatusResponse> resumeStoreOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody StoreOrderReasonRequest request) {
        return ApiResponse.ok(StoreOrderStatusResponse.from(storeOrderUseCase.resumeStoreOrder(
                new StoreOrderResumeCommand(orderId, request.reason(), principal.userId()), principal)));
    }

    @PatchMapping("/{orderId}/complete-partial")
    public ApiResponse<StoreOrderCompletePartialResponse> completePartialStoreOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody StoreOrderReasonRequest request) {
        return ApiResponse.ok(StoreOrderCompletePartialResponse.from(storeOrderUseCase.completePartialStoreOrder(
                new StoreOrderCompletePartialCommand(orderId, request.reason(), principal.userId()), principal)));
    }
}

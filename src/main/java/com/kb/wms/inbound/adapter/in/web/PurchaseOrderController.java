package com.kb.wms.inbound.adapter.in.web;

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
import com.kb.wms.inbound.adapter.in.web.dto.request.PurchaseOrderCancelRequest;
import com.kb.wms.inbound.adapter.in.web.dto.request.PurchaseOrderRegisterRequest;
import com.kb.wms.inbound.adapter.in.web.dto.response.PurchaseOrderDetailsResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.PurchaseOrderRegisterResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.PurchaseOrderResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.PurchaseOrderStatusResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.PurchaseOrderSummaryResponse;
import com.kb.wms.inbound.application.port.in.PurchaseOrderUseCase;
import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 발주 등록/조회/확정/취소.
 * POST, GET /api/v1/purchase-orders, GET .../{id}, GET .../{id}/details, PATCH .../{id}/confirm, PATCH .../{id}/cancel
 *
 * <p>처리 사용자는 토큰 주체이고, 역할은 SecurityConfig가, 담당 창고 범위와 취소 권한(작성자·상태별)은 서비스가 검사한다.
 * 목록은 페이지네이션 없이 전체를 반환한다(공통 페이징 도입 시 추가).
 */
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderUseCase purchaseOrderUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PurchaseOrderRegisterResponse> registerPurchaseOrder(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PurchaseOrderRegisterRequest request) {
        Long purchaseOrderId = purchaseOrderUseCase.registerPurchaseOrder(request.toCommand(principal.userId()), principal);
        PurchaseOrderView view = purchaseOrderUseCase.getPurchaseOrder(purchaseOrderId, principal);
        List<PurchaseOrderLineView> lines =
                purchaseOrderUseCase.getPurchaseOrderDetails(purchaseOrderId, principal).items();
        return ApiResponse.created(PurchaseOrderRegisterResponse.of(view, lines));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<PurchaseOrderSummaryResponse>> getPurchaseOrders(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {
        List<PurchaseOrderSummaryResponse> items = purchaseOrderUseCase
                .getPurchaseOrders(PurchaseOrderSearchCondition.unscoped(
                        status, warehouseId, supplierId, keyword, createdFrom, createdTo), principal)
                .stream()
                .map(PurchaseOrderSummaryResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{purchaseOrderId}")
    public ApiResponse<PurchaseOrderResponse> getPurchaseOrder(@AuthenticationPrincipal AuthenticatedUser principal,
                                                            @PathVariable Long purchaseOrderId) {
        return ApiResponse.ok(PurchaseOrderResponse.from(
                purchaseOrderUseCase.getPurchaseOrder(purchaseOrderId, principal)));
    }

    @GetMapping("/{purchaseOrderId}/details")
    public ApiResponse<PurchaseOrderDetailsResponse> getPurchaseOrderDetails(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long purchaseOrderId) {
        return ApiResponse.ok(PurchaseOrderDetailsResponse.from(
                purchaseOrderUseCase.getPurchaseOrderDetails(purchaseOrderId, principal)));
    }

    @PatchMapping("/{purchaseOrderId}/confirm")
    public ApiResponse<PurchaseOrderStatusResponse> confirmPurchaseOrder(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long purchaseOrderId) {
        return ApiResponse.ok(PurchaseOrderStatusResponse.from(
                purchaseOrderUseCase.confirmPurchaseOrder(purchaseOrderId, principal.userId())));
    }

    @PatchMapping("/{purchaseOrderId}/cancel")
    public ApiResponse<PurchaseOrderStatusResponse> cancelPurchaseOrder(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long purchaseOrderId,
            @Valid @RequestBody(required = false) PurchaseOrderCancelRequest request) {
        PurchaseOrderCancelRequest body = request != null ? request : new PurchaseOrderCancelRequest(null);
        PurchaseOrder canceled = purchaseOrderUseCase.cancelPurchaseOrder(
                purchaseOrderId, body.toCommand(principal.userId()), principal);
        String cancelReason = purchaseOrderUseCase.getPurchaseOrder(purchaseOrderId, principal).cancelReason();
        return ApiResponse.ok(PurchaseOrderStatusResponse.of(canceled, cancelReason));
    }
}

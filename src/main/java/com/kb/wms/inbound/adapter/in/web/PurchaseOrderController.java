package com.kb.wms.inbound.adapter.in.web;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
 * <p>인증/인가가 아직 구현되지 않아 역할별 규칙(등록은 담당 창고 관리자, 확정은 본사 관리자, 취소는 상태별 권한자,
 * 창고 관리자의 담당 창고 범위 조회)은 적용하지 않는다. 등록·확정·취소의 처리 사용자(userId)는 쿼리 파라미터로 받으며
 * (상태 이력의 처리자로 기록), 인증 연동 시 토큰의 사용자로 대체하고 이 컨트롤러에서 역할 검사를 추가한다.
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
            @RequestParam Long userId,
            @Valid @RequestBody PurchaseOrderRegisterRequest request) {
        Long purchaseOrderId = purchaseOrderUseCase.registerPurchaseOrder(request.toCommand(userId));
        PurchaseOrderView view = purchaseOrderUseCase.getPurchaseOrder(purchaseOrderId);
        List<PurchaseOrderLineView> lines = purchaseOrderUseCase.getPurchaseOrderDetails(purchaseOrderId).items();
        return ApiResponse.created(PurchaseOrderRegisterResponse.of(view, lines));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<PurchaseOrderSummaryResponse>> getPurchaseOrders(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {
        List<PurchaseOrderSummaryResponse> items = purchaseOrderUseCase
                .getPurchaseOrders(new PurchaseOrderSearchCondition(
                        status, warehouseId, supplierId, keyword, createdFrom, createdTo))
                .stream()
                .map(PurchaseOrderSummaryResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{purchaseOrderId}")
    public ApiResponse<PurchaseOrderResponse> getPurchaseOrder(@PathVariable Long purchaseOrderId) {
        return ApiResponse.ok(PurchaseOrderResponse.from(purchaseOrderUseCase.getPurchaseOrder(purchaseOrderId)));
    }

    @GetMapping("/{purchaseOrderId}/details")
    public ApiResponse<PurchaseOrderDetailsResponse> getPurchaseOrderDetails(@PathVariable Long purchaseOrderId) {
        return ApiResponse.ok(PurchaseOrderDetailsResponse.from(
                purchaseOrderUseCase.getPurchaseOrderDetails(purchaseOrderId)));
    }

    @PatchMapping("/{purchaseOrderId}/confirm")
    public ApiResponse<PurchaseOrderStatusResponse> confirmPurchaseOrder(
            @PathVariable Long purchaseOrderId,
            @RequestParam Long userId) {
        return ApiResponse.ok(PurchaseOrderStatusResponse.from(
                purchaseOrderUseCase.confirmPurchaseOrder(purchaseOrderId, userId)));
    }

    @PatchMapping("/{purchaseOrderId}/cancel")
    public ApiResponse<PurchaseOrderStatusResponse> cancelPurchaseOrder(
            @PathVariable Long purchaseOrderId,
            @RequestParam Long userId,
            @Valid @RequestBody(required = false) PurchaseOrderCancelRequest request) {
        PurchaseOrderCancelRequest body = request != null ? request : new PurchaseOrderCancelRequest(null);
        PurchaseOrder canceled = purchaseOrderUseCase.cancelPurchaseOrder(purchaseOrderId, body.toCommand(userId));
        String cancelReason = purchaseOrderUseCase.getPurchaseOrder(purchaseOrderId).cancelReason();
        return ApiResponse.ok(PurchaseOrderStatusResponse.of(canceled, cancelReason));
    }
}

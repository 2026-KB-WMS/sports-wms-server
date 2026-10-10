package com.kb.wms.outbound.adapter.in.web;

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
import com.kb.wms.outbound.adapter.in.web.dto.request.OutboundCreateRequest;
import com.kb.wms.outbound.adapter.in.web.dto.request.OutboundPickingCompleteRequest;
import com.kb.wms.outbound.adapter.in.web.dto.request.OutboundReasonRequest;
import com.kb.wms.outbound.adapter.in.web.dto.response.OutboundCancelResponse;
import com.kb.wms.outbound.adapter.in.web.dto.response.OutboundCreateResponse;
import com.kb.wms.outbound.adapter.in.web.dto.response.OutboundDeliverResponse;
import com.kb.wms.outbound.adapter.in.web.dto.response.OutboundDetailResponse;
import com.kb.wms.outbound.adapter.in.web.dto.response.OutboundListItemResponse;
import com.kb.wms.outbound.adapter.in.web.dto.response.OutboundPickingCompleteResponse;
import com.kb.wms.outbound.adapter.in.web.dto.response.OutboundPickingStartResponse;
import com.kb.wms.outbound.adapter.in.web.dto.response.OutboundShipResponse;
import com.kb.wms.outbound.application.port.in.OutboundFulfillmentUseCase;
import com.kb.wms.outbound.application.port.in.OutboundUseCase;
import com.kb.wms.outbound.application.port.in.command.OutboundCancelCommand;
import com.kb.wms.outbound.application.port.in.query.OutboundSearchCondition;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 출고 생성/목록/상세/피킹 시작·완료/배송 시작·완료/취소.
 * POST, GET /api/v1/outbounds, GET .../{outboundId}/details,
 * PATCH .../{outboundId}/picking/start, picking/complete, ship, deliver, cancel
 *
 * <p>처리 사용자는 토큰 주체이고, 역할(쓰기는 창고 관리자, 조회는 본사와 창고 관리자)은 SecurityConfig가,
 * 담당 창고 범위(발주에 배정된 창고)는 서비스가 검사한다. 목록은 페이지네이션 없이 전체를 반환한다.
 */
@Tag(name = "출고")
@RestController
@RequestMapping("/api/v1/outbounds")
@RequiredArgsConstructor
public class OutboundController {

    private final OutboundUseCase outboundUseCase;
    private final OutboundFulfillmentUseCase outboundFulfillmentUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OutboundCreateResponse> createOutbound(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody OutboundCreateRequest request) {
        return ApiResponse.created(OutboundCreateResponse.from(
                outboundUseCase.createOutbound(request.toCommand(principal.userId()), principal)));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<OutboundListItemResponse>> getOutbounds(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) OutboundStatus status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long storeOrderId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {
        List<OutboundListItemResponse> items = outboundUseCase
                .searchOutbounds(OutboundSearchCondition.unscoped(
                        status, warehouseId, storeId, storeOrderId, keyword, createdFrom, createdTo), principal)
                .stream()
                .map(OutboundListItemResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{outboundId}/details")
    public ApiResponse<OutboundDetailResponse> getOutboundDetails(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long outboundId) {
        return ApiResponse.ok(OutboundDetailResponse.from(outboundUseCase.getOutbound(outboundId, principal)));
    }

    @PatchMapping("/{outboundId}/picking/start")
    public ApiResponse<OutboundPickingStartResponse> startPicking(
            @PathVariable Long outboundId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(OutboundPickingStartResponse.from(outboundUseCase.startPicking(outboundId, principal)));
    }

    @PatchMapping("/{outboundId}/picking/complete")
    public ApiResponse<OutboundPickingCompleteResponse> completePicking(
            @PathVariable Long outboundId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody OutboundPickingCompleteRequest request) {
        return ApiResponse.ok(OutboundPickingCompleteResponse.from(
                outboundFulfillmentUseCase.completePicking(request.toCommand(outboundId, principal.userId()), principal)));
    }

    @PatchMapping("/{outboundId}/ship")
    public ApiResponse<OutboundShipResponse> ship(
            @PathVariable Long outboundId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(OutboundShipResponse.from(outboundFulfillmentUseCase.ship(outboundId, principal)));
    }

    @PatchMapping("/{outboundId}/deliver")
    public ApiResponse<OutboundDeliverResponse> deliver(
            @PathVariable Long outboundId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(OutboundDeliverResponse.from(outboundFulfillmentUseCase.deliver(outboundId, principal)));
    }

    @PatchMapping("/{outboundId}/cancel")
    public ApiResponse<OutboundCancelResponse> cancelOutbound(
            @PathVariable Long outboundId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody OutboundReasonRequest request) {
        return ApiResponse.ok(OutboundCancelResponse.from(outboundUseCase.cancel(
                new OutboundCancelCommand(outboundId, request.reason(), principal.userId()), principal)));
    }
}

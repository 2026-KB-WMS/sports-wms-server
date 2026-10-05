package com.kb.wms.outbound.adapter.in.web;

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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 출고 생성/목록/상세/피킹 시작·완료/배송 시작·완료/취소.
 * POST, GET /api/v1/outbounds, GET .../{outboundId}/details,
 * PATCH .../{outboundId}/picking/start, picking/complete, ship, deliver, cancel
 *
 * <p>인증/인가가 아직 구현되지 않아 역할별 규칙(쓰기는 담당 창고 관리자, 조회는 본사 관리자와 담당 창고 관리자,
 * 데이터 범위 제한, 공급 단가·금액 노출 범위)은 적용하지 않는다. 처리 사용자(userId)는 쿼리 파라미터로 받으며,
 * 인증 연동 시 토큰의 사용자로 대체하고 이 컨트롤러에서 역할 검사를 추가한다. 목록은 페이지네이션 없이 전체를 반환한다.
 */
@RestController
@RequestMapping("/api/v1/outbounds")
@RequiredArgsConstructor
public class OutboundController {

    private final OutboundUseCase outboundUseCase;
    private final OutboundFulfillmentUseCase outboundFulfillmentUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OutboundCreateResponse> createOutbound(
            @RequestParam Long userId,
            @Valid @RequestBody OutboundCreateRequest request) {
        return ApiResponse.created(OutboundCreateResponse.from(
                outboundUseCase.createOutbound(request.toCommand(userId))));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<OutboundListItemResponse>> getOutbounds(
            @RequestParam(required = false) OutboundStatus status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long storeOrderId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {
        List<OutboundListItemResponse> items = outboundUseCase
                .searchOutbounds(new OutboundSearchCondition(
                        status, warehouseId, storeId, storeOrderId, keyword, createdFrom, createdTo))
                .stream()
                .map(OutboundListItemResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{outboundId}/details")
    public ApiResponse<OutboundDetailResponse> getOutboundDetails(@PathVariable Long outboundId) {
        return ApiResponse.ok(OutboundDetailResponse.from(outboundUseCase.getOutbound(outboundId)));
    }

    @PatchMapping("/{outboundId}/picking/start")
    public ApiResponse<OutboundPickingStartResponse> startPicking(
            @PathVariable Long outboundId,
            @RequestParam Long userId) {
        return ApiResponse.ok(OutboundPickingStartResponse.from(outboundUseCase.startPicking(outboundId, userId)));
    }

    @PatchMapping("/{outboundId}/picking/complete")
    public ApiResponse<OutboundPickingCompleteResponse> completePicking(
            @PathVariable Long outboundId,
            @RequestParam Long userId,
            @Valid @RequestBody OutboundPickingCompleteRequest request) {
        return ApiResponse.ok(OutboundPickingCompleteResponse.from(
                outboundFulfillmentUseCase.completePicking(request.toCommand(outboundId, userId))));
    }

    @PatchMapping("/{outboundId}/ship")
    public ApiResponse<OutboundShipResponse> ship(
            @PathVariable Long outboundId,
            @RequestParam Long userId) {
        return ApiResponse.ok(OutboundShipResponse.from(outboundFulfillmentUseCase.ship(outboundId, userId)));
    }

    @PatchMapping("/{outboundId}/deliver")
    public ApiResponse<OutboundDeliverResponse> deliver(
            @PathVariable Long outboundId,
            @RequestParam Long userId) {
        return ApiResponse.ok(OutboundDeliverResponse.from(outboundFulfillmentUseCase.deliver(outboundId, userId)));
    }

    @PatchMapping("/{outboundId}/cancel")
    public ApiResponse<OutboundCancelResponse> cancelOutbound(
            @PathVariable Long outboundId,
            @RequestParam Long userId,
            @Valid @RequestBody OutboundReasonRequest request) {
        return ApiResponse.ok(OutboundCancelResponse.from(outboundUseCase.cancel(
                new OutboundCancelCommand(outboundId, request.reason(), userId))));
    }
}

package com.kb.wms.inbound.adapter.in.web;

import java.math.BigDecimal;
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
import com.kb.wms.inbound.adapter.in.web.dto.request.InboundCancelRequest;
import com.kb.wms.inbound.adapter.in.web.dto.request.InboundInspectRequest;
import com.kb.wms.inbound.adapter.in.web.dto.request.InboundRegisterRequest;
import com.kb.wms.inbound.adapter.in.web.dto.response.InboundCancelResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.InboundCompleteResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.InboundDetailsResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.InboundInspectResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.InboundRegisterResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.InboundResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.InboundSummaryResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.SectionCandidatesResponse;
import com.kb.wms.inbound.application.port.in.InboundCompleteUseCase;
import com.kb.wms.inbound.application.port.in.InboundInspectUseCase;
import com.kb.wms.inbound.application.port.in.InboundUseCase;
import com.kb.wms.inbound.application.port.in.query.InboundSearchCondition;
import com.kb.wms.inbound.application.port.in.query.SectionCandidateCondition;
import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.enums.InboundStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 입고 등록/조회/검수/완료/취소와 검수 구역 후보 조회.
 * POST, GET /api/v1/inbounds, GET .../{id}, GET .../{id}/details, PATCH .../{id}/inspect, PATCH .../{id}/complete,
 * PATCH .../{id}/cancel, GET .../{id}/assignable-sections, GET .../{id}/defect-sections
 *
 * <p>인증/인가가 아직 구현되지 않아 역할별 규칙(등록·검수·완료·취소는 담당 창고 관리자, 조회는 본사 관리자 또는
 * 담당 창고 관리자)은 적용하지 않는다. 등록·검수·완료·취소의 처리 사용자(userId)는 쿼리 파라미터로 받으며
 * (상태 이력의 처리자로 기록), 인증 연동 시 토큰의 사용자로 대체하고 이 컨트롤러에서 역할·소속 창고 검사를 추가한다.
 * 목록은 페이지네이션 없이 전체를 반환한다(공통 페이징 도입 시 추가).
 */
@RestController
@RequestMapping("/api/v1/inbounds")
@RequiredArgsConstructor
public class InboundController {

    private final InboundUseCase inboundUseCase;
    private final InboundInspectUseCase inboundInspectUseCase;
    private final InboundCompleteUseCase inboundCompleteUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InboundRegisterResponse> registerInbound(
            @RequestParam Long userId,
            @Valid @RequestBody InboundRegisterRequest request) {
        Long inboundId = inboundUseCase.registerInbound(request.toCommand(userId));
        return ApiResponse.created(InboundRegisterResponse.from(inboundUseCase.getInbound(inboundId)));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<InboundSummaryResponse>> getInbounds(
            @RequestParam(required = false) InboundStatus status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long purchaseOrderId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivedTo) {
        List<InboundSummaryResponse> items = inboundUseCase
                .getInbounds(new InboundSearchCondition(
                        status, warehouseId, purchaseOrderId, keyword, arrivedFrom, arrivedTo))
                .stream()
                .map(InboundSummaryResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{inboundId}")
    public ApiResponse<InboundResponse> getInbound(@PathVariable Long inboundId) {
        return ApiResponse.ok(InboundResponse.from(inboundUseCase.getInbound(inboundId)));
    }

    @GetMapping("/{inboundId}/details")
    public ApiResponse<InboundDetailsResponse> getInboundDetails(@PathVariable Long inboundId) {
        return ApiResponse.ok(InboundDetailsResponse.from(inboundUseCase.getInboundDetails(inboundId)));
    }

    @PatchMapping("/{inboundId}/inspect")
    public ApiResponse<InboundInspectResponse> inspectInbound(
            @PathVariable Long inboundId,
            @RequestParam Long userId,
            @Valid @RequestBody InboundInspectRequest request) {
        Inbound inbound = inboundInspectUseCase.inspectInbound(inboundId, request.toCommand(userId));
        return ApiResponse.ok(InboundInspectResponse.of(
                inbound, inboundUseCase.getInboundDetails(inboundId).items()));
    }

    @PatchMapping("/{inboundId}/complete")
    public ApiResponse<InboundCompleteResponse> completeInbound(
            @PathVariable Long inboundId,
            @RequestParam Long userId) {
        return ApiResponse.ok(InboundCompleteResponse.from(
                inboundCompleteUseCase.completeInbound(inboundId, userId)));
    }

    @PatchMapping("/{inboundId}/cancel")
    public ApiResponse<InboundCancelResponse> cancelInbound(
            @PathVariable Long inboundId,
            @RequestParam Long userId,
            @Valid @RequestBody InboundCancelRequest request) {
        Inbound inbound = inboundUseCase.cancelInbound(inboundId, request.toCommand(userId));
        return ApiResponse.ok(InboundCancelResponse.of(inbound, inboundUseCase.getInbound(inboundId)));
    }

    @GetMapping("/{inboundId}/assignable-sections")
    public ApiResponse<SectionCandidatesResponse> getAssignableSections(
            @PathVariable Long inboundId,
            @RequestParam(required = false) BigDecimal requiredQuantity,
            @RequestParam(required = false) String keyword) {
        InboundView inbound = inboundUseCase.getInbound(inboundId);
        return ApiResponse.ok(SectionCandidatesResponse.of(
                inboundId, inbound.warehouseId(),
                inboundUseCase.getAssignableSections(inboundId, new SectionCandidateCondition(requiredQuantity, keyword))));
    }

    @GetMapping("/{inboundId}/defect-sections")
    public ApiResponse<SectionCandidatesResponse> getDefectSections(
            @PathVariable Long inboundId,
            @RequestParam(required = false) BigDecimal requiredQuantity,
            @RequestParam(required = false) String keyword) {
        InboundView inbound = inboundUseCase.getInbound(inboundId);
        return ApiResponse.ok(SectionCandidatesResponse.of(
                inboundId, inbound.warehouseId(),
                inboundUseCase.getDefectSections(inboundId, new SectionCandidateCondition(requiredQuantity, keyword))));
    }
}

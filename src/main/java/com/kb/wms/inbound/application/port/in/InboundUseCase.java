package com.kb.wms.inbound.application.port.in;

import java.util.List;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.inbound.application.port.in.command.InboundCancelCommand;
import com.kb.wms.inbound.application.port.in.command.InboundRegisterCommand;
import com.kb.wms.inbound.application.port.in.query.InboundSearchCondition;
import com.kb.wms.inbound.application.port.in.query.SectionCandidateCondition;
import com.kb.wms.inbound.application.port.in.result.InboundDetails;
import com.kb.wms.inbound.application.port.in.result.InboundSummary;
import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.application.port.in.result.SectionCandidate;
import com.kb.wms.inbound.domain.entity.Inbound;

/**
 * 입고 등록/조회/취소와 검수용 구역 후보 조회 유스케이스. 입고 창고가 actor의 담당 창고(본사는 조회만 전체)가 아니면 403 FORBIDDEN이다.
 * POST, GET /api/v1/inbounds, GET .../{id}, GET .../{id}/details, GET .../{id}/assignable-sections,
 * GET .../{id}/defect-sections, PATCH .../{id}/cancel
 * (검수 PATCH .../{id}/inspect, 완료 PATCH .../{id}/complete는 각각 별도 유스케이스로 둔다.)
 */
public interface InboundUseCase {

    /** 확정된 발주에 대한 도착(ARRIVED) 입고를 등록하고 입고 ID를 반환한다. 응답 조립은 조회 유스케이스로 한다. */
    Long registerInbound(InboundRegisterCommand command, AuthenticatedUser actor);

    List<InboundSummary> getInbounds(InboundSearchCondition condition, AuthenticatedUser actor);

    InboundView getInbound(Long inboundId, AuthenticatedUser actor);

    InboundDetails getInboundDetails(Long inboundId, AuthenticatedUser actor);

    /** 완료 전(ARRIVED·INSPECTING)의 입고를 사유와 함께 취소한다. */
    Inbound cancelInbound(Long inboundId, InboundCancelCommand command, AuthenticatedUser actor);

    /** 입고 창고의 활성 구역 중 합격품을 둘 수 있는(불량 구역이 아니고 용량 여유가 있는) 구역 후보. */
    List<SectionCandidate> getAssignableSections(Long inboundId, SectionCandidateCondition condition, AuthenticatedUser actor);

    /** 입고 창고의 활성 불량 구역(DEFECT) 중 용량 여유가 있는 구역 후보. */
    List<SectionCandidate> getDefectSections(Long inboundId, SectionCandidateCondition condition, AuthenticatedUser actor);
}

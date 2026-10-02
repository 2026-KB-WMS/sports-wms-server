package com.kb.wms.inbound.application.port.in;

import java.util.List;

import com.kb.wms.inbound.application.port.in.command.InboundCancelCommand;
import com.kb.wms.inbound.application.port.in.command.InboundInspectCommand;
import com.kb.wms.inbound.application.port.in.command.InboundRegisterCommand;
import com.kb.wms.inbound.application.port.in.query.InboundSearchCondition;
import com.kb.wms.inbound.application.port.in.query.SectionCandidateCondition;
import com.kb.wms.inbound.application.port.in.result.InboundCompleteResult;
import com.kb.wms.inbound.application.port.in.result.InboundDetails;
import com.kb.wms.inbound.application.port.in.result.InboundSummary;
import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.application.port.in.result.SectionCandidate;
import com.kb.wms.inbound.domain.entity.Inbound;

/**
 * 입고 등록/조회/검수/완료/취소와 검수용 구역 후보 조회 유스케이스.
 * POST, GET /api/v1/inbounds, GET .../{id}, GET .../{id}/details, GET .../{id}/assignable-sections,
 * GET .../{id}/defect-sections, PATCH .../{id}/inspect, PATCH .../{id}/complete, PATCH .../{id}/cancel
 */
public interface InboundUseCase {

    /** 확정된 발주에 대한 도착(ARRIVED) 입고를 등록하고 입고 ID를 반환한다. 응답 조립은 조회 유스케이스로 한다. */
    Long registerInbound(InboundRegisterCommand command);

    List<InboundSummary> getInbounds(InboundSearchCondition condition);

    InboundView getInbound(Long inboundId);

    InboundDetails getInboundDetails(Long inboundId);

    /** 검수 항목 전체를 교체하고(재호출 가능) 처음이면 입고를 INSPECTING으로 전환한다. 재고에는 반영하지 않는다. */
    Inbound inspectInbound(Long inboundId, InboundInspectCommand command);

    /** 입고를 완료(COMPLETED)하고 재고·재고 이력·발주 상태를 한 트랜잭션으로 반영한다. */
    InboundCompleteResult completeInbound(Long inboundId, Long userId);

    /** 완료 전(ARRIVED·INSPECTING)의 입고를 사유와 함께 취소한다. */
    Inbound cancelInbound(Long inboundId, InboundCancelCommand command);

    /** 입고 창고의 활성 구역 중 합격품을 둘 수 있는(불량 구역이 아니고 용량 여유가 있는) 구역 후보. */
    List<SectionCandidate> getAssignableSections(Long inboundId, SectionCandidateCondition condition);

    /** 입고 창고의 활성 불량 구역(DEFECT) 중 용량 여유가 있는 구역 후보. */
    List<SectionCandidate> getDefectSections(Long inboundId, SectionCandidateCondition condition);
}

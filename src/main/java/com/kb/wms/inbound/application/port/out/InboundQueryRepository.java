package com.kb.wms.inbound.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.inbound.application.port.in.query.InboundSearchCondition;
import com.kb.wms.inbound.application.port.in.query.SectionCandidateCondition;
import com.kb.wms.inbound.application.port.in.result.InboundLineView;
import com.kb.wms.inbound.application.port.in.result.InboundSummary;
import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.application.port.in.result.SectionCandidate;

/**
 * 입고 조회 전용 아웃바운드 포트. 발주·공급처·창고·SKU·로트·구역 정보를 ID로 조인한 조회 결과를 돌려준다(ADR-007).
 */
public interface InboundQueryRepository {

    /** 도착 일시 최신순(같으면 입고 ID 내림차순)으로 정렬한다. 검수 항목이 없는 입고도 포함한다. */
    List<InboundSummary> search(InboundSearchCondition condition);

    Optional<InboundView> findView(Long inboundId);

    /** 검수 항목을 SKU 코드·로트 번호 순으로 돌려준다. */
    List<InboundLineView> findLineViews(Long inboundId);

    /** 창고의 활성 구역 중 불량 구역이 아니고 가용 용량이 0보다 큰 구역(구역 코드순). */
    List<SectionCandidate> findAssignableSections(Long warehouseId, SectionCandidateCondition condition);

    /** 창고의 활성 불량 구역(DEFECT) 중 가용 용량이 0보다 큰 구역(구역 코드순). */
    List<SectionCandidate> findDefectSections(Long warehouseId, SectionCandidateCondition condition);
}

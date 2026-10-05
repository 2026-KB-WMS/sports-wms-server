package com.kb.wms.outbound.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.kb.wms.outbound.application.port.in.query.OutboundSearchCondition;
import com.kb.wms.outbound.application.port.in.query.StockAllocationSearchCondition;
import com.kb.wms.outbound.application.port.in.result.FefoStockCandidate;
import com.kb.wms.outbound.application.port.in.result.OutboundLineView;
import com.kb.wms.outbound.application.port.in.result.OutboundSummary;
import com.kb.wms.outbound.application.port.in.result.OutboundView;
import com.kb.wms.outbound.application.port.in.result.StockAllocationSummary;
import com.kb.wms.outbound.application.port.in.result.StockAllocationView;

/**
 * 출고·재고 할당 조회 전용 아웃바운드 포트. 지점·창고·SKU·로트·구역 정보는 ID 기준 읽기 전용 조인으로 가져온다(ADR-007).
 * 상태를 바꾸는 흐름은 {@link OutboundRepository}·{@link StockAllocationRepository}를 쓴다.
 */
public interface OutboundQueryRepository {

    /** 할당 목록. 할당 일시 내림차순, 같으면 할당 ID 내림차순. */
    List<StockAllocationSummary> searchAllocations(StockAllocationSearchCondition condition);

    /** 할당 ID 목록의 요약(할당 ID 오름차순). 할당 생성 응답에 쓴다. 빈 컬렉션이면 빈 목록. */
    List<StockAllocationSummary> findAllocationSummaries(Collection<Long> allocationIds);

    Optional<StockAllocationView> findAllocationView(Long allocationId);

    /** 할당이 연결된, 취소되지 않은 출고의 ID. 연결이 없으면 비어 있다. */
    Optional<Long> findActiveOutboundIdByAllocationId(Long allocationId);

    /** 출고 목록. 생성 일시 내림차순, 같으면 출고 ID 내림차순. */
    List<OutboundSummary> searchOutbounds(OutboundSearchCondition condition);

    Optional<OutboundView> findOutboundView(Long outboundId);

    /** 출고 항목 상세(항목 ID 오름차순). */
    List<OutboundLineView> findOutboundLineViews(Long outboundId);

    /**
     * 대상 창고에서 SKU별 FEFO 후보 재고 행을 우선순위 순으로 돌려준다. 품질 AVAILABLE, 로트 AVAILABLE, 구역 ACTIVE이고
     * 가용 수량(보유 - 할당)이 양수인 행만이며, 유통기한이 빠른 순(없으면 뒤) → 로트 생성이 빠른 순 → 재고 행 ID 순이다.
     * 잠그지 않는 조회이므로 실제 예약은 재고 유스케이스가 잠그고 다시 검증한다.
     */
    List<FefoStockCandidate> findFefoCandidates(Long warehouseId, Collection<Long> skuIds);
}

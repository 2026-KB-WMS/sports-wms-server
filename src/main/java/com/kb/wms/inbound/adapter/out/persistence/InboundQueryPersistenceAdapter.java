package com.kb.wms.inbound.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.common.persistence.SearchKeyword;
import com.kb.wms.inbound.adapter.out.persistence.repository.InboundJpaRepository;
import com.kb.wms.inbound.adapter.out.persistence.repository.InboundLineJpaRepository;
import com.kb.wms.inbound.application.port.in.query.InboundSearchCondition;
import com.kb.wms.inbound.application.port.in.query.SectionCandidateCondition;
import com.kb.wms.inbound.application.port.in.result.InboundLineView;
import com.kb.wms.inbound.application.port.in.result.InboundSummary;
import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.application.port.in.result.SectionCandidate;
import com.kb.wms.inbound.application.port.out.InboundQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InboundQueryPersistenceAdapter implements InboundQueryRepository {

    private final InboundJpaRepository inboundJpaRepository;
    private final InboundLineJpaRepository inboundLineJpaRepository;

    /** JPQL의 IN에 빈 목록을 넘기지 않으려는 자리 값. 범위 조건을 쓰지 않거나 담당 창고가 없을 때 쓴다. */
    private static final List<Long> NO_WAREHOUSE = List.of(-1L);

    private static List<Long> scopeIds(List<Long> warehouseIds) {
        return warehouseIds == null || warehouseIds.isEmpty() ? NO_WAREHOUSE : warehouseIds;
    }

    @Override
    public List<InboundSummary> search(InboundSearchCondition condition) {
        return inboundJpaRepository.search(
                condition.status(), condition.warehouseId(), condition.purchaseOrderId(),
                SearchKeyword.normalize(condition.keyword()),
                condition.arrivedFrom(), condition.arrivedTo(),
                condition.warehouseIds() != null, scopeIds(condition.warehouseIds()));
    }

    @Override
    public Optional<InboundView> findView(Long inboundId) {
        return inboundJpaRepository.findView(inboundId);
    }

    @Override
    public List<InboundLineView> findLineViews(Long inboundId) {
        return inboundLineJpaRepository.findLineViews(inboundId);
    }

    @Override
    public List<SectionCandidate> findAssignableSections(Long warehouseId, SectionCandidateCondition condition) {
        return inboundJpaRepository.findAssignableSections(
                warehouseId, condition.requiredQuantity(), SearchKeyword.normalize(condition.keyword()));
    }

    @Override
    public List<SectionCandidate> findDefectSections(Long warehouseId, SectionCandidateCondition condition) {
        return inboundJpaRepository.findDefectSections(
                warehouseId, condition.requiredQuantity(), SearchKeyword.normalize(condition.keyword()));
    }
}

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

    @Override
    public List<InboundSummary> search(InboundSearchCondition condition) {
        return inboundJpaRepository.search(
                condition.status(), condition.warehouseId(), condition.purchaseOrderId(),
                SearchKeyword.normalize(condition.keyword()),
                condition.arrivedFrom(), condition.arrivedTo());
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

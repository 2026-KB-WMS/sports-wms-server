package com.kb.wms.outbound.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.common.persistence.SearchKeyword;
import com.kb.wms.outbound.adapter.out.persistence.repository.OutboundQueryJpaRepository;
import com.kb.wms.outbound.adapter.out.persistence.repository.StockAllocationQueryJpaRepository;
import com.kb.wms.outbound.application.port.in.query.OutboundSearchCondition;
import com.kb.wms.outbound.application.port.in.query.StockAllocationSearchCondition;
import com.kb.wms.outbound.application.port.in.result.FefoStockCandidate;
import com.kb.wms.outbound.application.port.in.result.OutboundLineView;
import com.kb.wms.outbound.application.port.in.result.OutboundSummary;
import com.kb.wms.outbound.application.port.in.result.OutboundView;
import com.kb.wms.outbound.application.port.in.result.StockAllocationSummary;
import com.kb.wms.outbound.application.port.in.result.StockAllocationView;
import com.kb.wms.outbound.application.port.out.OutboundQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboundQueryPersistenceAdapter implements OutboundQueryRepository {

    private final StockAllocationQueryJpaRepository allocationQueryRepository;
    private final OutboundQueryJpaRepository outboundQueryRepository;

    @Override
    public List<StockAllocationSummary> searchAllocations(StockAllocationSearchCondition condition) {
        return allocationQueryRepository.search(
                condition.storeOrderId(), condition.warehouseId(), condition.skuId(), condition.status(),
                SearchKeyword.normalize(condition.keyword()));
    }

    @Override
    public List<StockAllocationSummary> findAllocationSummaries(Collection<Long> allocationIds) {
        if (allocationIds.isEmpty()) {
            return List.of();
        }
        return allocationQueryRepository.findSummariesByIds(allocationIds);
    }

    @Override
    public Optional<StockAllocationView> findAllocationView(Long allocationId) {
        return allocationQueryRepository.findView(allocationId);
    }

    @Override
    public Optional<Long> findActiveOutboundIdByAllocationId(Long allocationId) {
        return allocationQueryRepository.findActiveOutboundIds(allocationId).stream().findFirst();
    }

    @Override
    public List<OutboundSummary> searchOutbounds(OutboundSearchCondition condition) {
        return outboundQueryRepository.search(
                condition.status(), condition.warehouseId(), condition.storeId(), condition.storeOrderId(),
                SearchKeyword.normalize(condition.keyword()), condition.createdFrom(), condition.createdTo());
    }

    @Override
    public Optional<OutboundView> findOutboundView(Long outboundId) {
        return outboundQueryRepository.findView(outboundId);
    }

    @Override
    public List<OutboundLineView> findOutboundLineViews(Long outboundId) {
        return outboundQueryRepository.findLineViews(outboundId);
    }

    @Override
    public List<FefoStockCandidate> findFefoCandidates(Long warehouseId, Collection<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return List.of();
        }
        return allocationQueryRepository.findFefoCandidates(warehouseId, skuIds);
    }
}

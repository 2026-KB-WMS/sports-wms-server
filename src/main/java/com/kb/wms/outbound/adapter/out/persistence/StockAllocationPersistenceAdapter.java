package com.kb.wms.outbound.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.outbound.adapter.out.persistence.entity.StockAllocationJpaEntity;
import com.kb.wms.outbound.adapter.out.persistence.repository.OutboundLineJpaRepository;
import com.kb.wms.outbound.adapter.out.persistence.repository.StockAllocationJpaRepository;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StockAllocationPersistenceAdapter implements StockAllocationRepository {

    private final StockAllocationJpaRepository stockAllocationJpaRepository;
    private final OutboundLineJpaRepository outboundLineJpaRepository;

    @Override
    public StockAllocation save(StockAllocation allocation) {
        return stockAllocationJpaRepository.save(StockAllocationJpaEntity.fromDomain(allocation)).toDomain();
    }

    @Override
    public List<StockAllocation> saveAll(List<StockAllocation> allocations) {
        List<StockAllocationJpaEntity> entities = allocations.stream()
                .map(StockAllocationJpaEntity::fromDomain)
                .toList();
        return stockAllocationJpaRepository.saveAll(entities).stream()
                .map(StockAllocationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<StockAllocation> findById(Long allocationId) {
        return stockAllocationJpaRepository.findById(allocationId).map(StockAllocationJpaEntity::toDomain);
    }

    @Override
    public Optional<StockAllocation> findByIdForUpdate(Long allocationId) {
        return stockAllocationJpaRepository.findByIdForUpdate(allocationId).map(StockAllocationJpaEntity::toDomain);
    }

    @Override
    public List<StockAllocation> findAllByIdForUpdate(Collection<Long> allocationIds) {
        if (allocationIds.isEmpty()) {
            return List.of();
        }
        return toDomains(stockAllocationJpaRepository.findAllByIdForUpdate(allocationIds));
    }

    @Override
    public List<StockAllocation> findByStoreOrderId(Long storeOrderId) {
        return toDomains(stockAllocationJpaRepository.findByStoreOrderId(storeOrderId));
    }

    @Override
    public List<StockAllocation> findByStoreOrderIdAndStatusForUpdate(Long storeOrderId, AllocationStatus status) {
        return toDomains(stockAllocationJpaRepository.findByStoreOrderIdAndStatusForUpdate(storeOrderId, status));
    }

    @Override
    public List<StockAllocation> findUnlinkedAllocatedByStoreOrderIdForUpdate(Long storeOrderId) {
        return toDomains(stockAllocationJpaRepository.findUnlinkedAllocatedByStoreOrderIdForUpdate(storeOrderId));
    }

    @Override
    public boolean existsByStoreOrderIdAndStatus(Long storeOrderId, AllocationStatus status) {
        return stockAllocationJpaRepository.existsByStoreOrderIdAndStatus(storeOrderId, status);
    }

    @Override
    public boolean isLinkedToActiveOutbound(Long allocationId) {
        return outboundLineJpaRepository.existsLinkedToActiveOutbound(allocationId);
    }

    private List<StockAllocation> toDomains(List<StockAllocationJpaEntity> entities) {
        return entities.stream().map(StockAllocationJpaEntity::toDomain).toList();
    }
}

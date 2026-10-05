package com.kb.wms.outbound.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.outbound.adapter.out.persistence.entity.OutboundJpaEntity;
import com.kb.wms.outbound.adapter.out.persistence.entity.OutboundLineJpaEntity;
import com.kb.wms.outbound.adapter.out.persistence.repository.OutboundJpaRepository;
import com.kb.wms.outbound.adapter.out.persistence.repository.OutboundLineJpaRepository;
import com.kb.wms.outbound.application.port.out.OutboundRepository;
import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.entity.OutboundLine;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboundPersistenceAdapter implements OutboundRepository {

    private final OutboundJpaRepository outboundJpaRepository;
    private final OutboundLineJpaRepository outboundLineJpaRepository;

    @Override
    public Outbound save(Outbound outbound) {
        return outboundJpaRepository.save(OutboundJpaEntity.fromDomain(outbound)).toDomain();
    }

    @Override
    public List<OutboundLine> saveLines(List<OutboundLine> lines) {
        List<OutboundLineJpaEntity> entities = lines.stream()
                .map(OutboundLineJpaEntity::fromDomain)
                .toList();
        return outboundLineJpaRepository.saveAll(entities).stream()
                .map(OutboundLineJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Outbound> findById(Long outboundId) {
        return outboundJpaRepository.findById(outboundId).map(OutboundJpaEntity::toDomain);
    }

    @Override
    public Optional<Outbound> findByIdForUpdate(Long outboundId) {
        return outboundJpaRepository.findByIdForUpdate(outboundId).map(OutboundJpaEntity::toDomain);
    }

    @Override
    public List<OutboundLine> findLinesByOutboundId(Long outboundId) {
        return outboundLineJpaRepository.findByOutboundIdOrderByOutboundLineIdAsc(outboundId).stream()
                .map(OutboundLineJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Outbound> findByStoreOrderId(Long storeOrderId) {
        return toDomains(outboundJpaRepository.findByStoreOrderIdOrderByOutboundIdAsc(storeOrderId));
    }

    @Override
    public List<Outbound> findByStoreOrderIds(Collection<Long> storeOrderIds) {
        if (storeOrderIds.isEmpty()) {
            return List.of();
        }
        return toDomains(outboundJpaRepository.findByStoreOrderIdInOrderByOutboundIdAsc(storeOrderIds));
    }

    @Override
    public List<Outbound> findByStoreOrderIdAndStatusForUpdate(Long storeOrderId, OutboundStatus status) {
        return toDomains(outboundJpaRepository.findByStoreOrderIdAndStatusForUpdate(storeOrderId, status));
    }

    @Override
    public boolean existsByStoreOrderIdAndStatusIn(Long storeOrderId, Collection<OutboundStatus> statuses) {
        return outboundJpaRepository.existsByStoreOrderIdAndStatusIn(storeOrderId, statuses);
    }

    @Override
    public boolean existsNotCanceledByStoreOrderId(Long storeOrderId) {
        return outboundJpaRepository.existsByStoreOrderIdAndStatusNot(storeOrderId, OutboundStatus.CANCELED);
    }

    @Override
    public boolean existsByOutboundNo(String outboundNo) {
        return outboundJpaRepository.existsByOutboundNo(outboundNo);
    }

    @Override
    public long countByOutboundNoPrefix(String prefix) {
        return outboundJpaRepository.countByOutboundNoStartingWith(prefix);
    }

    private List<Outbound> toDomains(List<OutboundJpaEntity> entities) {
        return entities.stream().map(OutboundJpaEntity::toDomain).toList();
    }
}

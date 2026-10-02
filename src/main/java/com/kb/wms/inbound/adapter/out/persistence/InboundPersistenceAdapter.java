package com.kb.wms.inbound.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.adapter.out.persistence.entity.InboundJpaEntity;
import com.kb.wms.inbound.adapter.out.persistence.entity.InboundLineJpaEntity;
import com.kb.wms.inbound.adapter.out.persistence.repository.InboundJpaRepository;
import com.kb.wms.inbound.adapter.out.persistence.repository.InboundLineJpaRepository;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.InboundLine;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InboundPersistenceAdapter implements InboundRepository {

    private final InboundJpaRepository inboundJpaRepository;
    private final InboundLineJpaRepository inboundLineJpaRepository;

    @Override
    public Inbound save(Inbound inbound) {
        return inboundJpaRepository.save(InboundJpaEntity.fromDomain(inbound)).toDomain();
    }

    @Override
    public Optional<Inbound> findById(Long inboundId) {
        return inboundJpaRepository.findById(inboundId).map(InboundJpaEntity::toDomain);
    }

    @Override
    public Optional<Inbound> findByIdForUpdate(Long inboundId) {
        return inboundJpaRepository.findByIdForUpdate(inboundId).map(InboundJpaEntity::toDomain);
    }

    @Override
    public List<InboundLine> saveLines(List<InboundLine> lines) {
        List<InboundLineJpaEntity> entities = lines.stream()
                .map(InboundLineJpaEntity::fromDomain)
                .toList();
        return inboundLineJpaRepository.saveAll(entities).stream()
                .map(InboundLineJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<InboundLine> findLinesByInboundId(Long inboundId) {
        return inboundLineJpaRepository.findByInboundIdOrderByInboundLineIdAsc(inboundId).stream()
                .map(InboundLineJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteLinesByInboundId(Long inboundId) {
        inboundLineJpaRepository.deleteByInboundId(inboundId);
    }

    @Override
    public boolean existsByInboundNo(String inboundNo) {
        return inboundJpaRepository.existsByInboundNo(inboundNo);
    }

    @Override
    public long countByInboundNoPrefix(String prefix) {
        return inboundJpaRepository.countByInboundNoStartingWith(prefix);
    }

    @Override
    public boolean existsInProgressByPurchaseOrderId(Long purchaseOrderId) {
        return inboundJpaRepository.existsInProgressByPurchaseOrderId(purchaseOrderId);
    }

    @Override
    public boolean existsNotCanceledByPurchaseOrderId(Long purchaseOrderId) {
        return inboundJpaRepository.existsNotCanceledByPurchaseOrderId(purchaseOrderId);
    }
}

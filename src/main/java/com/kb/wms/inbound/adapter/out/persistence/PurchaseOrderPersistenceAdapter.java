package com.kb.wms.inbound.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.adapter.out.persistence.entity.PurchaseOrderJpaEntity;
import com.kb.wms.inbound.adapter.out.persistence.entity.PurchaseOrderLineJpaEntity;
import com.kb.wms.inbound.adapter.out.persistence.repository.PurchaseOrderJpaRepository;
import com.kb.wms.inbound.adapter.out.persistence.repository.PurchaseOrderLineJpaRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PurchaseOrderPersistenceAdapter implements PurchaseOrderRepository {

    private final PurchaseOrderJpaRepository purchaseOrderJpaRepository;
    private final PurchaseOrderLineJpaRepository purchaseOrderLineJpaRepository;

    @Override
    public PurchaseOrder save(PurchaseOrder purchaseOrder) {
        return purchaseOrderJpaRepository.save(PurchaseOrderJpaEntity.fromDomain(purchaseOrder)).toDomain();
    }

    @Override
    public List<PurchaseOrderLine> saveLines(List<PurchaseOrderLine> lines) {
        List<PurchaseOrderLineJpaEntity> entities = lines.stream()
                .map(PurchaseOrderLineJpaEntity::fromDomain)
                .toList();
        return purchaseOrderLineJpaRepository.saveAll(entities).stream()
                .map(PurchaseOrderLineJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<PurchaseOrder> findById(Long purchaseOrderId) {
        return purchaseOrderJpaRepository.findById(purchaseOrderId).map(PurchaseOrderJpaEntity::toDomain);
    }

    @Override
    public List<PurchaseOrderLine> findLinesByPurchaseOrderId(Long purchaseOrderId) {
        return purchaseOrderLineJpaRepository.findByPurchaseOrderIdOrderByPurchaseOrderLineIdAsc(purchaseOrderId)
                .stream()
                .map(PurchaseOrderLineJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByPurchaseOrderNo(String purchaseOrderNo) {
        return purchaseOrderJpaRepository.existsByPurchaseOrderNo(purchaseOrderNo);
    }

    @Override
    public boolean existsInProgressBySupplierId(Long supplierId) {
        return purchaseOrderJpaRepository.existsInProgressBySupplierId(supplierId);
    }
}

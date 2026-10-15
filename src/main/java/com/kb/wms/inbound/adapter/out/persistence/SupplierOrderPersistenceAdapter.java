package com.kb.wms.inbound.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.adapter.out.persistence.entity.SupplierOrderJpaEntity;
import com.kb.wms.inbound.adapter.out.persistence.repository.SupplierOrderJpaRepository;
import com.kb.wms.inbound.application.port.out.SupplierOrderRepository;
import com.kb.wms.inbound.domain.entity.SupplierOrder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SupplierOrderPersistenceAdapter implements SupplierOrderRepository {

    private final SupplierOrderJpaRepository supplierOrderJpaRepository;

    @Override
    public SupplierOrder save(SupplierOrder supplierOrder) {
        return supplierOrderJpaRepository.save(SupplierOrderJpaEntity.fromDomain(supplierOrder)).toDomain();
    }

    @Override
    public Optional<SupplierOrder> findById(Long supplierOrderId) {
        return supplierOrderJpaRepository.findById(supplierOrderId).map(SupplierOrderJpaEntity::toDomain);
    }

    @Override
    public Optional<SupplierOrder> findByIdForUpdate(Long supplierOrderId) {
        return supplierOrderJpaRepository.findByIdForUpdate(supplierOrderId).map(SupplierOrderJpaEntity::toDomain);
    }

    @Override
    public List<SupplierOrder> findByPurchaseOrderId(Long purchaseOrderId) {
        return supplierOrderJpaRepository.findByPurchaseOrderIdOrderBySupplierOrderIdAsc(purchaseOrderId).stream()
                .map(SupplierOrderJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<SupplierOrder> findByPurchaseOrderIdForUpdate(Long purchaseOrderId) {
        return supplierOrderJpaRepository.findByPurchaseOrderIdForUpdate(purchaseOrderId).stream()
                .map(SupplierOrderJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySupplierOrderNo(String supplierOrderNo) {
        return supplierOrderJpaRepository.existsBySupplierOrderNo(supplierOrderNo);
    }

    @Override
    public long countBySupplierOrderNoPrefix(String prefix) {
        return supplierOrderJpaRepository.countBySupplierOrderNoStartingWith(prefix);
    }

    @Override
    public boolean existsInProgressBySupplierId(Long supplierId) {
        return supplierOrderJpaRepository.existsInProgressBySupplierId(supplierId);
    }
}

package com.kb.wms.inbound.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.common.persistence.SearchKeyword;
import com.kb.wms.inbound.adapter.out.persistence.entity.SupplierJpaEntity;
import com.kb.wms.inbound.adapter.out.persistence.repository.SupplierJpaRepository;
import com.kb.wms.inbound.application.port.in.query.SupplierSearchCondition;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.enums.SupplierStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SupplierPersistenceAdapter implements SupplierRepository {

    private final SupplierJpaRepository supplierJpaRepository;

    @Override
    public Supplier save(Supplier supplier) {
        SupplierJpaEntity saved = supplierJpaRepository.save(SupplierJpaEntity.fromDomain(supplier));
        return saved.toDomain();
    }

    @Override
    public Optional<Supplier> findById(Long supplierId) {
        return supplierJpaRepository.findById(supplierId).map(SupplierJpaEntity::toDomain);
    }

    @Override
    public List<Supplier> search(SupplierSearchCondition condition) {
        return supplierJpaRepository.search(
                        SearchKeyword.normalize(condition.keyword()),
                        SupplierStatus.fromActiveFlag(condition.isActive()))
                .stream()
                .map(SupplierJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(Long supplierId) {
        return supplierJpaRepository.existsById(supplierId);
    }

    @Override
    public boolean existsBySupplierCode(String supplierCode) {
        return supplierJpaRepository.existsBySupplierCode(supplierCode);
    }
}

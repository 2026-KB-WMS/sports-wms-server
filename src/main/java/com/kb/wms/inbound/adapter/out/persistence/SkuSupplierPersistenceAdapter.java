package com.kb.wms.inbound.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.adapter.out.persistence.entity.SkuSupplierJpaEntity;
import com.kb.wms.inbound.adapter.out.persistence.repository.SkuSupplierJpaRepository;
import com.kb.wms.inbound.application.port.out.SkuSupplierRepository;
import com.kb.wms.inbound.domain.entity.SkuSupplier;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SkuSupplierPersistenceAdapter implements SkuSupplierRepository {

    private final SkuSupplierJpaRepository skuSupplierJpaRepository;

    @Override
    public SkuSupplier save(SkuSupplier skuSupplier) {
        return skuSupplierJpaRepository.save(SkuSupplierJpaEntity.fromDomain(skuSupplier)).toDomain();
    }

    @Override
    public List<SkuSupplier> findBySkuId(Long skuId) {
        return skuSupplierJpaRepository.findBySkuIdOrderBySkuSupplierIdAsc(skuId).stream()
                .map(SkuSupplierJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<SkuSupplier> findBySkuIdIn(Collection<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        return skuSupplierJpaRepository.findBySkuIdIn(skuIds).stream()
                .map(SkuSupplierJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySkuIdAndSupplierId(Long skuId, Long supplierId) {
        return skuSupplierJpaRepository.existsBySkuIdAndSupplierId(skuId, supplierId);
    }

    @Override
    public long deleteBySkuIdAndSupplierId(Long skuId, Long supplierId) {
        return skuSupplierJpaRepository.deleteBySkuIdAndSupplierId(skuId, supplierId);
    }
}

package com.kb.wms.inventory.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.repository.LotJpaRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.Lot;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LotPersistenceAdapter implements LotRepository {

    private final LotJpaRepository lotJpaRepository;

    @Override
    public Lot save(Lot lot) {
        return lotJpaRepository.save(LotJpaEntity.fromDomain(lot)).toDomain();
    }

    @Override
    public Optional<Lot> findById(Long lotId) {
        return lotJpaRepository.findById(lotId).map(LotJpaEntity::toDomain);
    }

    @Override
    public Optional<Lot> findBySkuIdAndSupplierIdAndLotNumber(Long skuId, Long supplierId, String lotNumber) {
        return lotJpaRepository.findBySkuIdAndSupplierIdAndLotNumber(skuId, supplierId, lotNumber)
                .map(LotJpaEntity::toDomain);
    }
}

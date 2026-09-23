package com.kb.wms.warehouse.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.domain.entity.Warehouse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WarehousePersistenceAdapter implements WarehouseRepository {

    private final WarehouseJpaRepository warehouseJpaRepository;

    @Override
    public Warehouse save(Warehouse warehouse) {
        WarehouseJpaEntity saved = warehouseJpaRepository.save(WarehouseJpaEntity.fromDomain(warehouse));
        return saved.toDomain();
    }

    @Override
    public Optional<Warehouse> findById(Long warehouseId) {
        return warehouseJpaRepository.findById(warehouseId).map(WarehouseJpaEntity::toDomain);
    }

    @Override
    public List<Warehouse> findAll() {
        return warehouseJpaRepository.findAll().stream()
                .map(WarehouseJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByWarehouseCode(String warehouseCode) {
        return warehouseJpaRepository.existsByWarehouseCode(warehouseCode);
    }
}

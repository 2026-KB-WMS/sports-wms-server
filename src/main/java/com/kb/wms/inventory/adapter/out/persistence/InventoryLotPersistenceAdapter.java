package com.kb.wms.inventory.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.domain.entity.InventoryLot;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InventoryLotPersistenceAdapter implements InventoryLotRepository {

    private final InventoryLotJpaRepository inventoryLotJpaRepository;

    @Override
    public InventoryLot save(InventoryLot inventoryLot) {
        return inventoryLotJpaRepository.save(InventoryLotJpaEntity.fromDomain(inventoryLot)).toDomain();
    }

    @Override
    public Optional<InventoryLot> findById(Long inventoryLotId) {
        return inventoryLotJpaRepository.findById(inventoryLotId).map(InventoryLotJpaEntity::toDomain);
    }

    @Override
    public Optional<InventoryLot> findByIdForUpdate(Long inventoryLotId) {
        return inventoryLotJpaRepository.findByIdForUpdate(inventoryLotId).map(InventoryLotJpaEntity::toDomain);
    }

    @Override
    public List<InventoryLot> findAllByIdsForUpdate(Collection<Long> inventoryLotIds) {
        if (inventoryLotIds == null || inventoryLotIds.isEmpty()) {
            return List.of();
        }
        return inventoryLotJpaRepository.findAllByIdInForUpdate(inventoryLotIds).stream()
                .map(InventoryLotJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<InventoryLot> findBySectionIdAndLotIdForUpdate(Long sectionId, Long lotId) {
        return inventoryLotJpaRepository.findBySectionIdAndLotIdForUpdate(sectionId, lotId)
                .map(InventoryLotJpaEntity::toDomain);
    }

    @Override
    public boolean existsById(Long inventoryLotId) {
        return inventoryLotJpaRepository.existsById(inventoryLotId);
    }
}

package com.kb.wms.inventory.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryTransactionJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryTransactionJpaRepository;
import com.kb.wms.inventory.application.port.out.InventoryTransactionRepository;
import com.kb.wms.inventory.domain.entity.InventoryTransaction;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InventoryTransactionPersistenceAdapter implements InventoryTransactionRepository {

    private final InventoryTransactionJpaRepository inventoryTransactionJpaRepository;

    @Override
    public InventoryTransaction save(InventoryTransaction transaction) {
        return inventoryTransactionJpaRepository.save(InventoryTransactionJpaEntity.fromDomain(transaction))
                .toDomain();
    }
}

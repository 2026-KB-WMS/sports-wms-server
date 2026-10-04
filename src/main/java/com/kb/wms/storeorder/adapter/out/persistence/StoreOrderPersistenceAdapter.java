package com.kb.wms.storeorder.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderJpaEntity;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderLineJpaEntity;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderJpaRepository;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderLineJpaRepository;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StoreOrderPersistenceAdapter implements StoreOrderRepository {

    private final StoreOrderJpaRepository storeOrderJpaRepository;
    private final StoreOrderLineJpaRepository storeOrderLineJpaRepository;

    @Override
    public StoreOrder save(StoreOrder storeOrder) {
        return storeOrderJpaRepository.save(StoreOrderJpaEntity.fromDomain(storeOrder)).toDomain();
    }

    @Override
    public List<StoreOrderLine> saveLines(List<StoreOrderLine> lines) {
        List<StoreOrderLineJpaEntity> entities = lines.stream()
                .map(StoreOrderLineJpaEntity::fromDomain)
                .toList();
        return storeOrderLineJpaRepository.saveAll(entities).stream()
                .map(StoreOrderLineJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<StoreOrder> findById(Long storeOrderId) {
        return storeOrderJpaRepository.findById(storeOrderId).map(StoreOrderJpaEntity::toDomain);
    }

    @Override
    public Optional<StoreOrder> findByIdForUpdate(Long storeOrderId) {
        return storeOrderJpaRepository.findByIdForUpdate(storeOrderId).map(StoreOrderJpaEntity::toDomain);
    }

    @Override
    public List<StoreOrderLine> findLinesByStoreOrderId(Long storeOrderId) {
        return storeOrderLineJpaRepository.findByStoreOrderIdOrderByStoreOrderLineIdAsc(storeOrderId).stream()
                .map(StoreOrderLineJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<StoreOrderLine> findLinesByStoreOrderIdForUpdate(Long storeOrderId) {
        return storeOrderLineJpaRepository.findByStoreOrderIdForUpdate(storeOrderId).stream()
                .map(StoreOrderLineJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByOrderNo(String orderNo) {
        return storeOrderJpaRepository.existsByOrderNo(orderNo);
    }

    @Override
    public long countByOrderNoPrefix(String prefix) {
        return storeOrderJpaRepository.countByOrderNoStartingWith(prefix);
    }
}

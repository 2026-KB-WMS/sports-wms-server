package com.kb.wms.store.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.common.persistence.SearchKeyword;
import com.kb.wms.store.adapter.out.persistence.entity.StoreJpaEntity;
import com.kb.wms.store.adapter.out.persistence.repository.StoreJpaRepository;
import com.kb.wms.store.application.port.in.query.StoreSearchCondition;
import com.kb.wms.store.application.port.out.StoreRepository;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.domain.enums.StoreStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StorePersistenceAdapter implements StoreRepository {

    private final StoreJpaRepository storeJpaRepository;

    @Override
    public Store save(Store store) {
        StoreJpaEntity saved = storeJpaRepository.save(StoreJpaEntity.fromDomain(store));
        return saved.toDomain();
    }

    @Override
    public Optional<Store> findById(Long storeId) {
        return storeJpaRepository.findById(storeId).map(StoreJpaEntity::toDomain);
    }

    @Override
    public List<Store> search(StoreSearchCondition condition) {
        return storeJpaRepository.search(
                        SearchKeyword.normalize(condition.keyword()),
                        StoreStatus.fromActiveFlag(condition.isActive()))
                .stream()
                .map(StoreJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(Long storeId) {
        return storeJpaRepository.existsById(storeId);
    }

    @Override
    public boolean existsByStoreCode(String storeCode) {
        return storeJpaRepository.existsByStoreCode(storeCode);
    }
}

package com.kb.wms.store.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.store.application.port.in.query.StoreSearchCondition;
import com.kb.wms.store.domain.entity.Store;

/**
 * 지점 영속성 아웃바운드 포트.
 */
public interface StoreRepository {

    Store save(Store store);

    Optional<Store> findById(Long storeId);

    /** 조건이 null이면 해당 조건은 무시한다. 생성 일시 내림차순. */
    List<Store> search(StoreSearchCondition condition);

    boolean existsById(Long storeId);

    boolean existsByStoreCode(String storeCode);
}

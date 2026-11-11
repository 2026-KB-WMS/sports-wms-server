package com.kb.wms.store.application.port.out;

import java.util.Optional;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.store.application.port.in.query.StoreSearchCondition;
import com.kb.wms.store.domain.entity.Store;

/**
 * 지점 영속성 아웃바운드 포트.
 */
public interface StoreRepository {

    Store save(Store store);

    Optional<Store> findById(Long storeId);

    /** 없는 ID는 결과에서 빠진다. 담당자 목록에서 지점명을 한 번에 읽을 때 쓴다. */
    java.util.List<Store> findByIdIn(java.util.Collection<Long> storeIds);

    /** 조건이 null이면 해당 조건은 무시한다. 정렬·페이지는 {@code page}가 정한다. */
    PageResult<Store> search(StoreSearchCondition condition, PageCondition page);

    boolean existsById(Long storeId);

    boolean existsByStoreCode(String storeCode);
}

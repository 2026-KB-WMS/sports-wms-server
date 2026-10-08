package com.kb.wms.storeorder.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.common.persistence.SearchKeyword;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderJpaRepository;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderLineJpaRepository;
import com.kb.wms.storeorder.application.port.in.query.StoreOrderSearchCondition;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderLineView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderSummary;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderView;
import com.kb.wms.storeorder.application.port.out.StoreOrderQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StoreOrderQueryPersistenceAdapter implements StoreOrderQueryRepository {

    private final StoreOrderJpaRepository storeOrderJpaRepository;
    private final StoreOrderLineJpaRepository storeOrderLineJpaRepository;

    @Override
    public List<StoreOrderSummary> search(StoreOrderSearchCondition condition) {
        return storeOrderJpaRepository.search(
                condition.status(), condition.storeId(), condition.warehouseId(),
                SearchKeyword.normalize(condition.keyword()),
                condition.requestedFrom(), condition.requestedTo(),
                condition.storeIds() != null, scopeIds(condition.storeIds()),
                condition.warehouseIds() != null, scopeIds(condition.warehouseIds()));
    }

    /** JPQL의 IN에 빈 목록을 넘기지 않으려는 자리 값. 범위 조건을 쓰지 않거나 담당 범위가 비었을 때 쓴다. */
    private static final List<Long> NO_ID = List.of(-1L);

    private static List<Long> scopeIds(List<Long> ids) {
        return ids == null || ids.isEmpty() ? NO_ID : ids;
    }

    @Override
    public Optional<StoreOrderView> findView(Long storeOrderId) {
        return storeOrderJpaRepository.findView(storeOrderId);
    }

    @Override
    public List<StoreOrderLineView> findLineViews(Long storeOrderId) {
        return storeOrderLineJpaRepository.findLineViews(storeOrderId);
    }
}

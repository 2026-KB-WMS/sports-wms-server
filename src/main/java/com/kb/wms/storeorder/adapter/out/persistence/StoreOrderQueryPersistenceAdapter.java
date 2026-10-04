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
                condition.requestedFrom(), condition.requestedTo());
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

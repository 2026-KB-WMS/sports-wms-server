package com.kb.wms.inbound.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.common.persistence.SearchKeyword;
import com.kb.wms.inbound.adapter.out.persistence.repository.PurchaseOrderJpaRepository;
import com.kb.wms.inbound.adapter.out.persistence.repository.PurchaseOrderLineJpaRepository;
import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.application.port.out.PurchaseOrderQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PurchaseOrderQueryPersistenceAdapter implements PurchaseOrderQueryRepository {

    private final PurchaseOrderJpaRepository purchaseOrderJpaRepository;
    private final PurchaseOrderLineJpaRepository purchaseOrderLineJpaRepository;

    @Override
    public List<PurchaseOrderSummary> search(PurchaseOrderSearchCondition condition) {
        return purchaseOrderJpaRepository.search(
                condition.status(), condition.warehouseId(), condition.supplierId(),
                SearchKeyword.normalize(condition.keyword()),
                condition.createdFrom(), condition.createdTo());
    }

    @Override
    public Optional<PurchaseOrderView> findView(Long purchaseOrderId) {
        return purchaseOrderJpaRepository.findView(purchaseOrderId);
    }

    @Override
    public List<PurchaseOrderLineView> findLineViews(Long purchaseOrderId) {
        return purchaseOrderLineJpaRepository.findLineViews(purchaseOrderId);
    }
}

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

    /** JPQL의 IN에 빈 목록을 넘기지 않으려는 자리 값. 범위 조건을 쓰지 않거나 담당 창고가 없을 때 쓴다. */
    private static final List<Long> NO_WAREHOUSE = List.of(-1L);

    private static List<Long> scopeIds(List<Long> warehouseIds) {
        return warehouseIds == null || warehouseIds.isEmpty() ? NO_WAREHOUSE : warehouseIds;
    }

    @Override
    public List<PurchaseOrderSummary> search(PurchaseOrderSearchCondition condition) {
        return purchaseOrderJpaRepository.search(
                condition.status(), condition.warehouseId(), condition.supplierId(),
                SearchKeyword.normalize(condition.keyword()),
                condition.createdFrom(), condition.createdTo(),
                condition.warehouseIds() != null, scopeIds(condition.warehouseIds()));
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

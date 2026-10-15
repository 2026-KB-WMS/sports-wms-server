package com.kb.wms.inbound.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.common.persistence.SearchKeyword;
import com.kb.wms.inbound.adapter.out.persistence.repository.SupplierOrderJpaRepository;
import com.kb.wms.inbound.application.port.in.query.SupplierOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderLineView;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderView;
import com.kb.wms.inbound.application.port.out.SupplierOrderQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SupplierOrderQueryPersistenceAdapter implements SupplierOrderQueryRepository {

    private final SupplierOrderJpaRepository supplierOrderJpaRepository;

    /** JPQL의 IN에 빈 목록을 넘기지 않으려는 자리 값. 범위 조건을 쓰지 않거나 담당 창고가 없을 때 쓴다. */
    private static final List<Long> NO_WAREHOUSE = List.of(-1L);

    private static List<Long> scopeIds(List<Long> warehouseIds) {
        return warehouseIds == null || warehouseIds.isEmpty() ? NO_WAREHOUSE : warehouseIds;
    }

    @Override
    public List<SupplierOrderSummary> search(SupplierOrderSearchCondition condition) {
        return supplierOrderJpaRepository.search(
                condition.status(), condition.warehouseId(), condition.supplierId(), condition.purchaseOrderId(),
                SearchKeyword.normalize(condition.keyword()),
                condition.createdFrom(), condition.createdTo(),
                condition.warehouseIds() != null, scopeIds(condition.warehouseIds()));
    }

    @Override
    public List<SupplierOrderSummary> findSummariesByPurchaseOrderId(Long purchaseOrderId) {
        return supplierOrderJpaRepository.findSummariesByPurchaseOrderId(purchaseOrderId);
    }

    @Override
    public Optional<SupplierOrderView> findView(Long supplierOrderId) {
        return supplierOrderJpaRepository.findView(supplierOrderId);
    }

    @Override
    public List<SupplierOrderLineView> findLineViews(Long supplierOrderId) {
        return supplierOrderJpaRepository.findLineViews(supplierOrderId);
    }
}

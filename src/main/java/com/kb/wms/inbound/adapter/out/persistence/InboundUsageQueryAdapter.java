package com.kb.wms.inbound.adapter.out.persistence;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.application.port.out.InboundUsageQueryPort;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 진행 중 업무 존재 여부를 센다(읽기 전용). 도메인 엔티티는 불러오지 않고 존재 여부만 조회한다.
 * 지점 발주 항목·헤더와의 조인은 ADR-007의 조회 전용 조인에 해당한다.
 */
@Component
public class InboundUsageQueryAdapter implements InboundUsageQueryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean existsInboundBySkuIds(Collection<Long> skuIds, Collection<InboundStatus> statuses) {
        return exists("""
                select count(i) from InboundJpaEntity i, InboundLineJpaEntity il, PurchaseOrderLineJpaEntity pl
                where il.inboundId = i.inboundId and pl.purchaseOrderLineId = il.purchaseOrderLineId
                  and pl.skuId in :skuIds and i.status in :statuses
                """, "skuIds", skuIds, statuses);
    }

    @Override
    public boolean existsInboundByWarehouse(Long warehouseId, Collection<InboundStatus> statuses) {
        return exists("""
                select count(i) from InboundJpaEntity i
                where i.warehouseId = :warehouseId and i.status in :statuses
                """, "warehouseId", warehouseId, statuses);
    }

    @Override
    public boolean existsPurchaseOrderBySkuIds(Collection<Long> skuIds, Collection<PurchaseOrderStatus> statuses) {
        return exists("""
                select count(p) from PurchaseOrderJpaEntity p, PurchaseOrderLineJpaEntity pl
                where pl.purchaseOrderId = p.purchaseOrderId
                  and pl.skuId in :skuIds and p.status in :statuses
                """, "skuIds", skuIds, statuses);
    }

    @Override
    public boolean existsPurchaseOrderByWarehouse(Long warehouseId, Collection<PurchaseOrderStatus> statuses) {
        return exists("""
                select count(p) from PurchaseOrderJpaEntity p
                where p.warehouseId = :warehouseId and p.status in :statuses
                """, "warehouseId", warehouseId, statuses);
    }

    private boolean exists(String jpql, String name, Object value, Collection<?> statuses) {
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter(name, value)
                .setParameter("statuses", statuses)
                .getSingleResult();
        return count != null && count > 0;
    }
}

package com.kb.wms.storeorder.adapter.out.persistence;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.kb.wms.storeorder.application.port.out.StoreOrderUsageQueryPort;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 진행 중 업무 존재 여부를 센다(읽기 전용). 도메인 엔티티는 불러오지 않고 존재 여부만 조회한다.
 * 지점 발주 항목·헤더와의 조인은 ADR-007의 조회 전용 조인에 해당한다.
 */
@Component
public class StoreOrderUsageQueryAdapter implements StoreOrderUsageQueryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean existsStoreOrderBySkuIds(Collection<Long> skuIds, Collection<StoreOrderStatus> statuses) {
        return exists("""
                select count(so) from StoreOrderJpaEntity so, StoreOrderLineJpaEntity l
                where l.storeOrderId = so.storeOrderId
                  and l.skuId in :skuIds and so.status in :statuses
                """, "skuIds", skuIds, statuses);
    }

    @Override
    public boolean existsStoreOrderByWarehouse(Long warehouseId, Collection<StoreOrderStatus> statuses) {
        return exists("""
                select count(so) from StoreOrderJpaEntity so
                where so.warehouseId = :warehouseId and so.status in :statuses
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

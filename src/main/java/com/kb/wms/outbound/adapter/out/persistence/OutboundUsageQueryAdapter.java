package com.kb.wms.outbound.adapter.out.persistence;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.kb.wms.outbound.application.port.out.OutboundUsageQueryPort;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 진행 중 업무 존재 여부를 센다(읽기 전용). 도메인 엔티티는 불러오지 않고 존재 여부만 조회한다.
 * 지점 발주 항목·헤더와의 조인은 ADR-007의 조회 전용 조인에 해당한다.
 */
@Component
public class OutboundUsageQueryAdapter implements OutboundUsageQueryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean existsOutboundBySkuIds(Collection<Long> skuIds, Collection<OutboundStatus> statuses) {
        return exists("""
                select count(o) from OutboundJpaEntity o, StoreOrderLineJpaEntity l
                where l.storeOrderId = o.storeOrderId
                  and l.skuId in :skuIds and o.status in :statuses
                """, "skuIds", skuIds, statuses);
    }

    @Override
    public boolean existsOutboundByWarehouse(Long warehouseId, Collection<OutboundStatus> statuses) {
        return exists("""
                select count(o) from OutboundJpaEntity o, StoreOrderJpaEntity so
                where o.storeOrderId = so.storeOrderId
                  and so.warehouseId = :warehouseId and o.status in :statuses
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

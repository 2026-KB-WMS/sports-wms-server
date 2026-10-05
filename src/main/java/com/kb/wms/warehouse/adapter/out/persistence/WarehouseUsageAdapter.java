package com.kb.wms.warehouse.adapter.out.persistence;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.warehouse.application.port.out.WarehouseUsagePort;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 창고 비활성화 전 진행 중 업무 확인(읽기 전용). ADR-007에 따라 조회 경로에 한해
 * 입고·발주·출고·지점 발주 테이블을 warehouse_id 기준으로 JPQL 직접 조회한다. 도메인 엔티티는 불러오지 않고 존재 여부만 센다.
 */
@Component
public class WarehouseUsageAdapter implements WarehouseUsagePort {

    private static final Set<InboundStatus> INBOUND_IN_PROGRESS =
            Set.of(InboundStatus.ARRIVED, InboundStatus.INSPECTING);
    private static final Set<PurchaseOrderStatus> PURCHASE_ORDER_IN_PROGRESS =
            Set.of(PurchaseOrderStatus.REQUESTED, PurchaseOrderStatus.CONFIRMED);
    private static final Set<OutboundStatus> OUTBOUND_IN_PROGRESS = Set.of(
            OutboundStatus.READY, OutboundStatus.PICKING, OutboundStatus.PICKED, OutboundStatus.SHIPPED);
    private static final Set<StoreOrderStatus> STORE_ORDER_IN_PROGRESS = Set.of(
            StoreOrderStatus.REQUESTED, StoreOrderStatus.APPROVED,
            StoreOrderStatus.ASSIGNED, StoreOrderStatus.ON_HOLD);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean hasInProgressInbounds(Long warehouseId) {
        return exists("""
                select count(i) from InboundJpaEntity i
                where i.warehouseId = :warehouseId and i.status in :statuses
                """, warehouseId, INBOUND_IN_PROGRESS);
    }

    @Override
    public boolean hasInProgressPurchaseOrders(Long warehouseId) {
        return exists("""
                select count(p) from PurchaseOrderJpaEntity p
                where p.warehouseId = :warehouseId and p.status in :statuses
                """, warehouseId, PURCHASE_ORDER_IN_PROGRESS);
    }

    @Override
    public boolean hasInProgressOutbounds(Long warehouseId) {
        return exists("""
                select count(o) from OutboundJpaEntity o, StoreOrderJpaEntity so
                where o.storeOrderId = so.storeOrderId
                  and so.warehouseId = :warehouseId and o.status in :statuses
                """, warehouseId, OUTBOUND_IN_PROGRESS);
    }

    @Override
    public boolean hasInProgressStoreOrders(Long warehouseId) {
        return exists("""
                select count(so) from StoreOrderJpaEntity so
                where so.warehouseId = :warehouseId and so.status in :statuses
                """, warehouseId, STORE_ORDER_IN_PROGRESS);
    }

    private boolean exists(String jpql, Long warehouseId, Set<?> statuses) {
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("warehouseId", warehouseId)
                .setParameter("statuses", statuses)
                .getSingleResult();
        return count != null && count > 0;
    }
}

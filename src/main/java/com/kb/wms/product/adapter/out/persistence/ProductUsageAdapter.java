package com.kb.wms.product.adapter.out.persistence;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.product.application.port.out.ProductUsagePort;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 상품 비활성화 전 사용 여부 확인(읽기 전용). ADR-007에 따라 조회 경로에 한해 재고·입고·발주·출고·지점 발주 테이블을
 * product_sku.product_id 기준으로 JPQL 직접 조회한다. 도메인 엔티티는 불러오지 않고 존재 여부만 센다.
 * 진행 중 상태 집합은 창고 비활성화 가드(WarehouseUsageAdapter)와 같다.
 */
@Component
public class ProductUsageAdapter implements ProductUsagePort {

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
    public boolean hasStock(Long productId) {
        return exists("""
                select count(il) from InventoryLotJpaEntity il, LotJpaEntity l, ProductSkuJpaEntity s
                where il.lotId = l.lotId and l.skuId = s.skuId and s.productId = :productId
                  and (il.onHandQuantity > 0 or il.allocatedQuantity > 0)
                """, productId, null);
    }

    @Override
    public boolean hasInProgressInbounds(Long productId) {
        return exists("""
                select count(i) from InboundJpaEntity i, InboundLineJpaEntity il,
                                     PurchaseOrderLineJpaEntity pl, ProductSkuJpaEntity s
                where il.inboundId = i.inboundId and pl.purchaseOrderLineId = il.purchaseOrderLineId
                  and s.skuId = pl.skuId and s.productId = :productId and i.status in :statuses
                """, productId, INBOUND_IN_PROGRESS);
    }

    @Override
    public boolean hasInProgressPurchaseOrders(Long productId) {
        return exists("""
                select count(p) from PurchaseOrderJpaEntity p, PurchaseOrderLineJpaEntity pl, ProductSkuJpaEntity s
                where pl.purchaseOrderId = p.purchaseOrderId
                  and s.skuId = pl.skuId and s.productId = :productId and p.status in :statuses
                """, productId, PURCHASE_ORDER_IN_PROGRESS);
    }

    @Override
    public boolean hasInProgressOutbounds(Long productId) {
        return exists("""
                select count(o) from OutboundJpaEntity o, StoreOrderLineJpaEntity l, ProductSkuJpaEntity s
                where l.storeOrderId = o.storeOrderId
                  and s.skuId = l.skuId and s.productId = :productId and o.status in :statuses
                """, productId, OUTBOUND_IN_PROGRESS);
    }

    @Override
    public boolean hasInProgressStoreOrders(Long productId) {
        return exists("""
                select count(so) from StoreOrderJpaEntity so, StoreOrderLineJpaEntity l, ProductSkuJpaEntity s
                where l.storeOrderId = so.storeOrderId
                  and s.skuId = l.skuId and s.productId = :productId and so.status in :statuses
                """, productId, STORE_ORDER_IN_PROGRESS);
    }

    private boolean exists(String jpql, Long productId, Set<?> statuses) {
        var query = entityManager.createQuery(jpql, Long.class).setParameter("productId", productId);
        if (statuses != null) {
            query.setParameter("statuses", statuses);
        }
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }
}

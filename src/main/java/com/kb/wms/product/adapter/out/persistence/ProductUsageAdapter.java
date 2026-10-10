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
 * 상품·SKU 비활성화 전 사용 여부 확인(읽기 전용). ADR-007에 따라 조회 경로에 한해 재고·입고·발주·출고·지점 발주 테이블을
 * product_sku 기준으로 JPQL 직접 조회한다. 도메인 엔티티는 불러오지 않고 존재 여부만 센다.
 * 진행 중 상태 집합은 창고 비활성화 가드(WarehouseUsageAdapter)와 같다.
 * 상품 단위와 SKU 단위는 SKU를 고르는 조건({@link #BY_PRODUCT}, {@link #BY_SKU})만 다르고 쿼리는 같다.
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

    private static final String BY_PRODUCT = "s.productId = :id";
    private static final String BY_SKU = "s.skuId = :id";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean hasStock(Long productId) {
        return hasStock(BY_PRODUCT, productId);
    }

    @Override
    public boolean hasInProgressInbounds(Long productId) {
        return hasInProgressInbounds(BY_PRODUCT, productId);
    }

    @Override
    public boolean hasInProgressPurchaseOrders(Long productId) {
        return hasInProgressPurchaseOrders(BY_PRODUCT, productId);
    }

    @Override
    public boolean hasInProgressOutbounds(Long productId) {
        return hasInProgressOutbounds(BY_PRODUCT, productId);
    }

    @Override
    public boolean hasInProgressStoreOrders(Long productId) {
        return hasInProgressStoreOrders(BY_PRODUCT, productId);
    }

    @Override
    public boolean isSkuInUse(Long skuId) {
        return hasStock(BY_SKU, skuId)
                || hasInProgressInbounds(BY_SKU, skuId)
                || hasInProgressPurchaseOrders(BY_SKU, skuId)
                || hasInProgressOutbounds(BY_SKU, skuId)
                || hasInProgressStoreOrders(BY_SKU, skuId);
    }

    private boolean hasStock(String skuFilter, Long id) {
        return exists("""
                select count(il) from InventoryLotJpaEntity il, LotJpaEntity l, ProductSkuJpaEntity s
                where il.lotId = l.lotId and l.skuId = s.skuId and %s
                  and (il.onHandQuantity > 0 or il.allocatedQuantity > 0)
                """.formatted(skuFilter), id, null);
    }

    private boolean hasInProgressInbounds(String skuFilter, Long id) {
        return exists("""
                select count(i) from InboundJpaEntity i, InboundLineJpaEntity il,
                                     PurchaseOrderLineJpaEntity pl, ProductSkuJpaEntity s
                where il.inboundId = i.inboundId and pl.purchaseOrderLineId = il.purchaseOrderLineId
                  and s.skuId = pl.skuId and %s and i.status in :statuses
                """.formatted(skuFilter), id, INBOUND_IN_PROGRESS);
    }

    private boolean hasInProgressPurchaseOrders(String skuFilter, Long id) {
        return exists("""
                select count(p) from PurchaseOrderJpaEntity p, PurchaseOrderLineJpaEntity pl, ProductSkuJpaEntity s
                where pl.purchaseOrderId = p.purchaseOrderId
                  and s.skuId = pl.skuId and %s and p.status in :statuses
                """.formatted(skuFilter), id, PURCHASE_ORDER_IN_PROGRESS);
    }

    private boolean hasInProgressOutbounds(String skuFilter, Long id) {
        return exists("""
                select count(o) from OutboundJpaEntity o, StoreOrderLineJpaEntity l, ProductSkuJpaEntity s
                where l.storeOrderId = o.storeOrderId
                  and s.skuId = l.skuId and %s and o.status in :statuses
                """.formatted(skuFilter), id, OUTBOUND_IN_PROGRESS);
    }

    private boolean hasInProgressStoreOrders(String skuFilter, Long id) {
        return exists("""
                select count(so) from StoreOrderJpaEntity so, StoreOrderLineJpaEntity l, ProductSkuJpaEntity s
                where l.storeOrderId = so.storeOrderId
                  and s.skuId = l.skuId and %s and so.status in :statuses
                """.formatted(skuFilter), id, STORE_ORDER_IN_PROGRESS);
    }

    private boolean exists(String jpql, Long id, Set<?> statuses) {
        var query = entityManager.createQuery(jpql, Long.class).setParameter("id", id);
        if (statuses != null) {
            query.setParameter("statuses", statuses);
        }
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }
}

package com.kb.wms.product.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.application.port.in.InboundUsageUseCase;
import com.kb.wms.outbound.application.port.in.OutboundUsageUseCase;
import com.kb.wms.product.application.port.out.ProductUsagePort;
import com.kb.wms.storeorder.application.port.in.StoreOrderUsageUseCase;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

/**
 * 상품·SKU 비활성화 전 사용 여부 확인(읽기 전용). 진행 중 입고·창고 발주·출고·지점 발주는 각 업무 도메인의 유스케이스가
 * 판단해 결과만 돌려주고, 재고 존재 여부만 ADR-007에 따라 product_sku 기준으로 JPQL 직접 조회한다.
 * 상품 단위는 상품의 SKU ID 목록으로 바꿔 SKU 단위와 같은 질문을 한다.
 */
@Component
@RequiredArgsConstructor
public class ProductUsageAdapter implements ProductUsagePort {

    private static final String BY_PRODUCT = "s.productId = :id";
    private static final String BY_SKU = "s.skuId = :id";

    private final InboundUsageUseCase inboundUsageUseCase;
    private final OutboundUsageUseCase outboundUsageUseCase;
    private final StoreOrderUsageUseCase storeOrderUsageUseCase;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean hasStock(Long productId) {
        return hasStock(BY_PRODUCT, productId);
    }

    @Override
    public boolean hasInProgressInbounds(Long productId) {
        return inboundUsageUseCase.hasInProgressInboundForSkus(skuIdsOf(productId));
    }

    @Override
    public boolean hasInProgressPurchaseOrders(Long productId) {
        return inboundUsageUseCase.hasInProgressPurchaseOrderForSkus(skuIdsOf(productId));
    }

    @Override
    public boolean hasInProgressOutbounds(Long productId) {
        return outboundUsageUseCase.hasInProgressOutboundForSkus(skuIdsOf(productId));
    }

    @Override
    public boolean hasInProgressStoreOrders(Long productId) {
        return storeOrderUsageUseCase.hasInProgressStoreOrderForSkus(skuIdsOf(productId));
    }

    @Override
    public boolean isSkuInUse(Long skuId) {
        List<Long> skuIds = List.of(skuId);
        return hasStock(BY_SKU, skuId)
                || inboundUsageUseCase.hasInProgressInboundForSkus(skuIds)
                || inboundUsageUseCase.hasInProgressPurchaseOrderForSkus(skuIds)
                || outboundUsageUseCase.hasInProgressOutboundForSkus(skuIds)
                || storeOrderUsageUseCase.hasInProgressStoreOrderForSkus(skuIds);
    }

    private List<Long> skuIdsOf(Long productId) {
        return entityManager.createQuery(
                        "select s.skuId from ProductSkuJpaEntity s where s.productId = :id", Long.class)
                .setParameter("id", productId)
                .getResultList();
    }

    private boolean hasStock(String skuFilter, Long id) {
        Long count = entityManager.createQuery("""
                select count(il) from InventoryLotJpaEntity il, LotJpaEntity l, ProductSkuJpaEntity s
                where il.lotId = l.lotId and l.skuId = s.skuId and %s
                  and (il.onHandQuantity > 0 or il.allocatedQuantity > 0)
                """.formatted(skuFilter), Long.class)
                .setParameter("id", id)
                .getSingleResult();
        return count != null && count > 0;
    }
}

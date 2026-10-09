package com.kb.wms.product.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.application.port.out.ProductUsagePort;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderJpaEntity;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderLineJpaEntity;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 상품 사용 여부 조회(JPQL 도메인 간 조인) 검증. 재고와 지점 발주는 데이터를 넣어 확인하고,
 * 나머지 조회는 데이터가 없을 때 false로 실행되는지(쿼리 문법·엔티티 이름)만 확인한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class ProductUsageAdapterTest {

    @Autowired ProductUsagePort productUsagePort;
    @PersistenceContext EntityManager em;

    private ProductSkuJpaEntity sku(Long productId, String code) {
        ProductSkuJpaEntity sku = ProductSkuJpaEntity.builder()
                .productId(productId).skuCode(code).name("SKU " + code).unit("EA")
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build();
        em.persist(sku);
        return sku;
    }

    private void stock(ProductSkuJpaEntity sku, long onHand, long allocated) {
        LotJpaEntity lot = LotJpaEntity.builder()
                .skuId(sku.getSkuId()).supplierId(1L).lotNumber("LOT-" + sku.getSkuCode())
                .status(LotStatus.AVAILABLE).unitCost(BigDecimal.valueOf(1000)).build();
        em.persist(lot);
        em.persist(InventoryLotJpaEntity.builder()
                .sectionId(1L).lotId(lot.getLotId()).onHandQuantity(onHand).allocatedQuantity(allocated)
                .qualityStatus(QualityStatus.AVAILABLE).build());
    }

    private void storeOrder(ProductSkuJpaEntity sku, String orderNo, StoreOrderStatus status) {
        StoreOrderJpaEntity order = StoreOrderJpaEntity.builder()
                .orderNo(orderNo).storeId(1L).warehouseId(1L).status(status)
                .requestedAt(LocalDateTime.of(2026, 10, 9, 9, 0)).createdBy(1L).build();
        em.persist(order);
        em.persist(StoreOrderLineJpaEntity.builder()
                .storeOrderId(order.getStoreOrderId()).skuId(sku.getSkuId()).requestedQuantity(5L)
                .allocatedQuantity(0L).shippedQuantity(0L)
                .requestedUnitSupplyPrice(BigDecimal.valueOf(1500)).status(StoreOrderLineStatus.REQUESTED).build());
    }

    @Test
    @DisplayName("상품의 SKU에 보유 또는 할당 수량이 있으면 재고가 있다고 본다")
    void hasStock() {
        ProductSkuJpaEntity onHand = sku(10L, "S-ON");
        ProductSkuJpaEntity allocatedOnly = sku(11L, "S-AL");
        ProductSkuJpaEntity empty = sku(12L, "S-EM");
        stock(onHand, 3L, 0L);
        stock(allocatedOnly, 0L, 2L);
        stock(empty, 0L, 0L);
        em.flush();

        assertThat(productUsagePort.hasStock(10L)).isTrue();
        assertThat(productUsagePort.hasStock(11L)).isTrue();
        assertThat(productUsagePort.hasStock(12L)).isFalse();
        assertThat(productUsagePort.hasStock(99L)).isFalse();
    }

    @Test
    @DisplayName("진행 중인 지점 발주 항목에 상품의 SKU가 있으면 사용 중, 종결된 발주는 무시한다")
    void hasInProgressStoreOrders() {
        ProductSkuJpaEntity open = sku(20L, "S-OPEN");
        ProductSkuJpaEntity done = sku(21L, "S-DONE");
        storeOrder(open, "SO-20261009-0001", StoreOrderStatus.ASSIGNED);
        storeOrder(done, "SO-20261009-0002", StoreOrderStatus.COMPLETED);
        em.flush();

        assertThat(productUsagePort.hasInProgressStoreOrders(20L)).isTrue();
        assertThat(productUsagePort.hasInProgressStoreOrders(21L)).isFalse();
    }

    @Test
    @DisplayName("관련 데이터가 없으면 입고·창고 발주·출고 조회는 false이고 쿼리는 정상 실행된다")
    void emptyQueries() {
        assertThat(productUsagePort.hasInProgressInbounds(1L)).isFalse();
        assertThat(productUsagePort.hasInProgressPurchaseOrders(1L)).isFalse();
        assertThat(productUsagePort.hasInProgressOutbounds(1L)).isFalse();
    }
}

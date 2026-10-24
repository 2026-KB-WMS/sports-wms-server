package com.kb.wms.dashboard.adapter.out.persistence.warehouse;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.dashboard.application.port.in.warehouse.result.CapacityUsage;
import com.kb.wms.dashboard.application.port.in.warehouse.result.InboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.LowStockSku;
import com.kb.wms.dashboard.application.port.in.warehouse.result.OutboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseInventoryAlerts;
import com.kb.wms.dashboard.application.port.out.warehouse.WarehouseDashboardQueryRepository;
import com.kb.wms.inbound.adapter.out.persistence.entity.InboundJpaEntity;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.outbound.adapter.out.persistence.entity.OutboundJpaEntity;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderJpaEntity;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 창고 관리자 홈 집계 쿼리 검증. 대상 창고(w1)와 다른 창고(w2)의 데이터를 함께 심어 창고가 섞이지 않는지,
 * 시작 전·취소·비활성 같은 제외 대상이 빠지는지 확인한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class WarehouseDashboardQueryPersistenceAdapterTest {

    private static final LocalDate LIMIT = LocalDate.of(2026, 10, 31);

    @Autowired private WarehouseDashboardQueryRepository repository;
    @PersistenceContext private EntityManager em;

    private Long w1;
    private Long w2;
    private Long w3Empty;
    private Long s1;
    private Long s2;
    private int seq;

    @BeforeEach
    void setUp() {
        w1 = warehouse("WH-1");
        w2 = warehouse("WH-2");
        w3Empty = warehouse("WH-3");
        s1 = section(w1, "S1", null, 100, 0, WarehouseStatus.ACTIVE);
        s2 = section(w2, "S2", null, 100, 0, WarehouseStatus.ACTIVE);
    }

    @Test
    @DisplayName("입고 작업 대기는 검수 전·검수 중만 세고 완료·취소·다른 창고는 뺀다")
    void inboundWorkCounts() {
        inbound(InboundStatus.ARRIVED, w1);
        inbound(InboundStatus.ARRIVED, w1);
        inbound(InboundStatus.INSPECTING, w1);
        inbound(InboundStatus.COMPLETED, w1);
        inbound(InboundStatus.CANCELED, w1);
        inbound(InboundStatus.ARRIVED, w2);
        inbound(InboundStatus.INSPECTING, w2);

        assertThat(repository.findInboundWorkCounts(w1)).isEqualTo(new InboundWorkCounts(2L, 1L));
        assertThat(repository.findInboundWorkCounts(w3Empty)).isEqualTo(new InboundWorkCounts(0L, 0L));
    }

    @Test
    @DisplayName("출고 작업 대기는 피킹 전·작업 중(PICKING+PICKED)·배송 중만 세고 완료·취소·다른 창고는 뺀다")
    void outboundWorkCounts() {
        outbound(OutboundStatus.READY, w1);
        outbound(OutboundStatus.READY, w1);
        outbound(OutboundStatus.PICKING, w1);
        outbound(OutboundStatus.PICKED, w1);
        outbound(OutboundStatus.SHIPPED, w1);
        outbound(OutboundStatus.DELIVERED, w1);
        outbound(OutboundStatus.CANCELED, w1);
        outbound(OutboundStatus.READY, w2);
        outbound(OutboundStatus.SHIPPED, w2);

        assertThat(repository.findOutboundWorkCounts(w1)).isEqualTo(new OutboundWorkCounts(2L, 2L, 1L));
        assertThat(repository.findOutboundWorkCounts(w3Empty)).isEqualTo(new OutboundWorkCounts(0L, 0L, 0L));
    }

    @Test
    @DisplayName("수용량은 활성 최상위 구역의 수용량 합, 적치량은 활성 구역 전체의 합이다")
    void capacityUsage() {
        Long root = section(w1, "R1", null, 100, 30, WarehouseStatus.ACTIVE);
        section(w1, "C1", root, 40, 10, WarehouseStatus.ACTIVE);
        section(w1, "R2", null, 50, 5, WarehouseStatus.ACTIVE);
        section(w1, "R3", null, 70, 9, WarehouseStatus.INACTIVE);
        section(w2, "OTHER", null, 999, 999, WarehouseStatus.ACTIVE);

        // setUp의 S1(100, 0)이 최상위 구역으로 함께 들어간다: 수용량 100+100+50, 적치량 0+30+10+5
        CapacityUsage usage = repository.findCapacityUsage(w1);

        assertThat(usage.capacity()).isEqualByComparingTo("250");
        assertThat(usage.currentCapacity()).isEqualByComparingTo("45");
        assertThat(usage.availableCapacity()).isEqualByComparingTo("205");
        assertThat(usage.usageRate()).isEqualByComparingTo("0.18");
    }

    @Test
    @DisplayName("구역이 없으면 수용량 0, 사용률은 null이다")
    void capacityUsage_noSections() {
        CapacityUsage usage = repository.findCapacityUsage(w3Empty);

        assertThat(usage.capacity()).isEqualByComparingTo("0");
        assertThat(usage.currentCapacity()).isEqualByComparingTo("0");
        assertThat(usage.availableCapacity()).isEqualByComparingTo("0");
        assertThat(usage.usageRate()).isNull();
    }

    @Test
    @DisplayName("부족 재고는 이 창고의 가용 재고만 보고 부족 폭이 큰 순서로 담는다")
    void lowStockSkus() {
        Long skuA = sku("A", 10L, ProductStatus.ACTIVE);   // 가용 3, 부족 7
        Long skuB = sku("B", 20L, ProductStatus.ACTIVE);   // 가용 2, 부족 18
        Long skuC = sku("C", 5L, ProductStatus.ACTIVE);    // 가용 5, 안전 재고와 같아 부족 아님
        sku("D", 10L, ProductStatus.INACTIVE);              // 비활성 제외
        sku("E", 0L, ProductStatus.ACTIVE);                 // 안전 재고 0 제외
        Long skuF = sku("F", 8L, ProductStatus.ACTIVE);    // 재고 없음, 부족 8
        Long skuG = sku("G", 8L, ProductStatus.ACTIVE);    // 재고 없음, 부족 8 (F와 같아 SKU ID 순)

        inventoryLot(s1, lot(skuA, LotStatus.AVAILABLE, null), 4L, 1L, QualityStatus.AVAILABLE);
        inventoryLot(s1, lot(skuA, LotStatus.QUARANTINED, null), 50L, 0L, QualityStatus.AVAILABLE); // 로트 격리
        inventoryLot(s1, lot(skuA, LotStatus.AVAILABLE, null), 50L, 0L, QualityStatus.DEFECTIVE);   // 불량 재고
        inventoryLot(s2, lot(skuA, LotStatus.AVAILABLE, null), 100L, 0L, QualityStatus.AVAILABLE);  // 다른 창고
        inventoryLot(s1, lot(skuB, LotStatus.AVAILABLE, null), 2L, 0L, QualityStatus.AVAILABLE);
        inventoryLot(s1, lot(skuC, LotStatus.AVAILABLE, null), 5L, 0L, QualityStatus.AVAILABLE);

        WarehouseInventoryAlerts alerts = repository.findInventoryAlerts(w1, LIMIT, 3);

        assertThat(alerts.lowStockSkuCount()).isEqualTo(4L);
        assertThat(alerts.lowStockSkus()).extracting(LowStockSku::skuId).containsExactly(skuB, skuF, skuG);
        assertThat(alerts.lowStockSkus().get(0))
                .isEqualTo(new LowStockSku(skuB, "SKU-B", "상품 B", 2L, 20L));
        assertThat(alerts.lowStockSkus().get(1).availableQuantity()).isZero();

        // 한도를 늘리면 가장 뒤에 부족 폭이 가장 작은 A(가용 3)가 들어온다
        assertThat(repository.findInventoryAlerts(w1, LIMIT, 10).lowStockSkus())
                .extracting(LowStockSku::skuId).containsExactly(skuB, skuF, skuG, skuA);
        // 다른 창고는 A만 충분(100)하고 나머지는 재고가 없어 모두 부족이다(부족 폭 20, 8, 8, 5)
        assertThat(repository.findInventoryAlerts(w2, LIMIT, 10).lowStockSkus())
                .extracting(LowStockSku::skuId).containsExactly(skuB, skuF, skuG, skuC);
    }

    @Test
    @DisplayName("유통기한 임박 로트는 재고가 남은 가용 로트 중 기준일 이내(당일 포함)만 이 창고에서 센다")
    void expiringLotCount() {
        Long sku = sku("X", 0L, ProductStatus.ACTIVE);
        inventoryLot(s1, lot(sku, LotStatus.AVAILABLE, LIMIT), 5L, 0L, QualityStatus.AVAILABLE);                 // 당일 포함
        inventoryLot(s1, lot(sku, LotStatus.AVAILABLE, LIMIT.plusDays(1)), 5L, 0L, QualityStatus.AVAILABLE);     // 하루 늦음
        inventoryLot(s1, lot(sku, LotStatus.AVAILABLE, LocalDate.of(2026, 10, 1)), 0L, 0L, QualityStatus.AVAILABLE); // 재고 없음
        inventoryLot(s1, lot(sku, LotStatus.QUARANTINED, LocalDate.of(2026, 10, 5)), 5L, 0L, QualityStatus.AVAILABLE); // 격리
        inventoryLot(s1, lot(sku, LotStatus.AVAILABLE, null), 5L, 0L, QualityStatus.AVAILABLE);                  // 유통기한 없음
        inventoryLot(s2, lot(sku, LotStatus.AVAILABLE, LocalDate.of(2026, 10, 10)), 5L, 0L, QualityStatus.AVAILABLE); // 다른 창고

        assertThat(repository.findInventoryAlerts(w1, LIMIT, 5).expiringLotCount()).isEqualTo(1L);
        assertThat(repository.findInventoryAlerts(w2, LIMIT, 5).expiringLotCount()).isEqualTo(1L);
        assertThat(repository.findInventoryAlerts(w3Empty, LIMIT, 5).expiringLotCount()).isZero();
    }

    // ---------- 픽스처 ----------

    private Long warehouse(String code) {
        WarehouseJpaEntity entity = WarehouseJpaEntity.builder()
                .warehouseCode(code).name(code).address("주소").contactNumber("02-123-4567")
                .totalCapacity(BigDecimal.valueOf(1000)).status(WarehouseStatus.ACTIVE).build();
        em.persist(entity);
        return entity.getWarehouseId();
    }

    private Long section(Long warehouseId, String code, Long parentSectionId, long capacity, long currentCapacity,
                         WarehouseStatus status) {
        WarehouseSectionJpaEntity entity = WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).parentSectionId(parentSectionId).sectionCode(code + "-" + warehouseId)
                .name(code).sectionType("STORAGE")
                .capacity(BigDecimal.valueOf(capacity)).currentCapacity(BigDecimal.valueOf(currentCapacity))
                .status(status).build();
        em.persist(entity);
        return entity.getSectionId();
    }

    private Long sku(String name, Long safetyStock, ProductStatus status) {
        ProductSkuJpaEntity entity = ProductSkuJpaEntity.builder()
                .productId(1L).skuCode("SKU-" + name).name("상품 " + name).unit("EA")
                .currentPurchasePrice(BigDecimal.TEN).currentSupplyPrice(BigDecimal.TEN)
                .safetyStockQuantity(safetyStock).status(status).build();
        em.persist(entity);
        return entity.getSkuId();
    }

    private Long lot(Long skuId, LotStatus status, LocalDate expiryDate) {
        LotJpaEntity entity = LotJpaEntity.builder()
                .skuId(skuId).supplierId(1L).lotNumber("LOT-" + (++seq)).expiryDate(expiryDate)
                .status(status).unitCost(BigDecimal.TEN).build();
        em.persist(entity);
        return entity.getLotId();
    }

    private void inventoryLot(Long sectionId, Long lotId, Long onHand, Long allocated, QualityStatus quality) {
        em.persist(InventoryLotJpaEntity.builder()
                .sectionId(sectionId).lotId(lotId).onHandQuantity(onHand).allocatedQuantity(allocated)
                .qualityStatus(quality).build());
    }

    private void inbound(InboundStatus status, Long warehouseId) {
        em.persist(InboundJpaEntity.builder()
                .inboundNo("IB-" + (++seq)).purchaseOrderId(1L).warehouseId(warehouseId).status(status)
                .arrivedAt(LocalDateTime.of(2026, 10, 1, 9, 0)).build());
    }

    /** 출고에는 창고 컬럼이 없어 담당 창고를 가진 지점 발주를 함께 만든다. */
    private void outbound(OutboundStatus status, Long warehouseId) {
        StoreOrderJpaEntity order = StoreOrderJpaEntity.builder()
                .orderNo("SO-" + (++seq)).storeId(1L).warehouseId(warehouseId).status(StoreOrderStatus.ASSIGNED)
                .requestedAt(LocalDateTime.of(2026, 10, 1, 9, 0)).createdBy(1L).build();
        em.persist(order);
        em.persist(OutboundJpaEntity.builder()
                .outboundNo("OB-" + (++seq)).storeOrderId(order.getStoreOrderId()).status(status).build());
    }
}

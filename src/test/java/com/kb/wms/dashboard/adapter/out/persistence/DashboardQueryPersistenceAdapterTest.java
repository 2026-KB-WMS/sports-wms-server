package com.kb.wms.dashboard.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.dashboard.application.port.in.result.FlowCount;
import com.kb.wms.dashboard.application.port.in.result.InventoryTotals;
import com.kb.wms.dashboard.application.port.in.result.MonthlyFlow;
import com.kb.wms.dashboard.application.port.in.result.PendingCounts;
import com.kb.wms.dashboard.application.port.in.result.TopOutboundSku;
import com.kb.wms.dashboard.application.port.in.result.WarehouseDashboardRow;
import com.kb.wms.dashboard.application.port.out.DashboardQueryRepository;
import com.kb.wms.inbound.adapter.out.persistence.entity.InboundJpaEntity;
import com.kb.wms.inbound.adapter.out.persistence.entity.InboundLineJpaEntity;
import com.kb.wms.inbound.adapter.out.persistence.entity.PurchaseOrderJpaEntity;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.outbound.adapter.out.persistence.entity.OutboundJpaEntity;
import com.kb.wms.outbound.adapter.out.persistence.entity.OutboundLineJpaEntity;
import com.kb.wms.outbound.adapter.out.persistence.entity.StockAllocationJpaEntity;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderJpaEntity;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderLineJpaEntity;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 대시보드 집계 쿼리 검증. 기준 월은 2026-10으로 고정하고, 월 경계와 제외 대상(시작 전·취소·격리·비활성)을 함께 심는다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class DashboardQueryPersistenceAdapterTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);

    @Autowired private DashboardQueryRepository repository;
    @PersistenceContext private EntityManager em;

    private Long w1;
    private Long w2;
    private Long w3Inactive;
    private Long skuA;
    private Long skuB;
    private Long skuD;
    private int seq;

    @BeforeEach
    void setUp() {
        w1 = warehouse("WH-1", WarehouseStatus.ACTIVE);
        w2 = warehouse("WH-2", WarehouseStatus.ACTIVE);
        w3Inactive = warehouse("WH-3", WarehouseStatus.INACTIVE);
        Long s1 = section(w1, "S1");
        Long s2 = section(w2, "S2");

        skuA = sku("A", 100L, ProductStatus.ACTIVE);
        skuB = sku("B", 0L, ProductStatus.ACTIVE);
        sku("C", 50L, ProductStatus.INACTIVE);
        skuD = sku("D", 10L, ProductStatus.ACTIVE);

        Long lotA1 = lot(skuA, LotStatus.AVAILABLE, LocalDate.of(2026, 11, 10));
        Long lotA2 = lot(skuA, LotStatus.QUARANTINED, LocalDate.of(2026, 10, 1));
        Long lotB = lot(skuB, LotStatus.AVAILABLE, null);
        Long lotD = lot(skuD, LotStatus.AVAILABLE, LocalDate.of(2026, 10, 20));
        Long lotE = lot(skuB, LotStatus.AVAILABLE, LocalDate.of(2026, 10, 1));

        inventoryLot(s1, lotA1, 60L, 10L);
        inventoryLot(s2, lotA1, 30L, 0L);
        inventoryLot(s1, lotA2, 500L, 0L);
        inventoryLot(s1, lotB, 20L, 0L);
        inventoryLot(s2, lotD, 20L, 0L);
        inventoryLot(s1, lotE, 0L, 0L);

        Long lotF = lot(skuB, LotStatus.DISPOSED, null);
        inventoryLot(s1, lotF, 40L, 0L);
        inventoryLot(s2, lotB, 7L, 0L, QualityStatus.DEFECTIVE);
        // 격리 로트의 불량 재고는 격리로만 센다(불량과 겹치지 않음)
        inventoryLot(s2, lotA2, 3L, 0L, QualityStatus.DEFECTIVE);
    }

    @Test
    @DisplayName("조치 대기는 기간과 무관하게 현재 시점 전체를 센다")
    void pendingCountsIgnoreMonth() {
        storeOrder(StoreOrderStatus.REQUESTED, null, LocalDateTime.of(2026, 10, 5, 9, 0));
        storeOrder(StoreOrderStatus.REQUESTED, null, LocalDateTime.of(2026, 10, 5, 10, 0));
        storeOrder(StoreOrderStatus.REQUESTED, null, LocalDateTime.of(2026, 9, 30, 23, 59, 59));
        storeOrder(StoreOrderStatus.APPROVED, null, LocalDateTime.of(2026, 10, 6, 9, 0));
        storeOrder(StoreOrderStatus.ASSIGNED, w1, LocalDateTime.of(2026, 10, 7, 9, 0));
        purchaseOrder(PurchaseOrderStatus.REQUESTED);
        purchaseOrder(PurchaseOrderStatus.CONFIRMED);
        flush();

        PendingCounts pending = repository.findPendingCounts();

        assertThat(pending.storeOrderRequestedCount()).isEqualTo(3L);
        assertThat(pending.purchaseOrderRequestedCount()).isEqualTo(1L);
        assertThat(pending.unassignedApprovedStoreOrderCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("재고 총량은 격리·폐기·불량을 빼고 따로 세며, 안전 재고 미달과 유통기한 임박을 센다")
    void inventoryTotals() {
        InventoryTotals totals = repository.findInventoryTotals(LocalDate.of(2026, 11, 15));

        // 60 + 30 + 20 + 20 (격리·폐기·불량 제외, 재고 0은 영향 없음)
        assertThat(totals.totalQuantity()).isEqualTo(130L);
        // 격리 500 + 3, 폐기 40, 불량 7 (격리 로트의 불량 3은 불량에 중복 집계하지 않음)
        assertThat(totals.quarantinedQuantity()).isEqualTo(503L);
        assertThat(totals.disposedQuantity()).isEqualTo(40L);
        assertThat(totals.defectiveQuantity()).isEqualTo(7L);
        // A, B, D (C는 비활성)
        assertThat(totals.activeSkuCount()).isEqualTo(3L);
        // A: 가용 50 + 30 = 80 < 100. D: 20 >= 10. B는 안전 재고 0이라 제외
        assertThat(totals.lowStockSkuCount()).isEqualTo(1L);
        // lotA1(11-10), lotD(10-20). lotE는 유통기한이 지났지만 재고 0이라 제외, lotB는 유통기한 없음
        assertThat(totals.expiringLotCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("유통기한 임박 기준일은 당일을 포함한다")
    void expiringLimitIsInclusive() {
        assertThat(repository.findInventoryTotals(LocalDate.of(2026, 11, 10)).expiringLotCount()).isEqualTo(2L);
        assertThat(repository.findInventoryTotals(LocalDate.of(2026, 11, 9)).expiringLotCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("입고는 검수 완료 시각이 속한 달만 완료로 세고, 검수 중은 월과 무관하게 진행중으로 센다")
    void inboundFlows() {
        seedInbounds();

        assertThat(repository.findInboundCompleted(OCT, null)).isEqualTo(new FlowCount(2L, 150L));
        assertThat(repository.findInboundCompleted(YearMonth.of(2026, 9), null)).isEqualTo(new FlowCount(1L, 70L));
        assertThat(repository.findInboundInProgress(null)).isEqualTo(new FlowCount(1L, 25L));
        assertThat(repository.findInboundCompleted(OCT, w2)).isEqualTo(new FlowCount(1L, 50L));
        assertThat(repository.findInboundInProgress(w2)).isEqualTo(new FlowCount(0L, 0L));
    }

    @Test
    @DisplayName("출고는 배송 완료 시각이 속한 달만 완료로 세고, 피킹 대기·취소는 세지 않는다")
    void outboundFlows() {
        seedOutbounds();

        assertThat(repository.findOutboundCompleted(OCT, null)).isEqualTo(new FlowCount(2L, 55L));
        assertThat(repository.findOutboundCompleted(YearMonth.of(2026, 9), null)).isEqualTo(new FlowCount(1L, 25L));
        // PICKING은 할당 수량(30), SHIPPED는 출고 수량(12)
        assertThat(repository.findOutboundInProgress(null)).isEqualTo(new FlowCount(2L, 42L));
        assertThat(repository.findOutboundCompleted(OCT, w2)).isEqualTo(new FlowCount(1L, 15L));
        assertThat(repository.findOutboundInProgress(w1)).isEqualTo(new FlowCount(1L, 30L));
    }

    @Test
    @DisplayName("지점 발주는 요청 시각이 속한 달의 상태별 건수만 센다")
    void storeOrdersByStatus() {
        storeOrder(StoreOrderStatus.REQUESTED, null, LocalDateTime.of(2026, 10, 1, 0, 0));
        storeOrder(StoreOrderStatus.REQUESTED, null, LocalDateTime.of(2026, 10, 31, 23, 59, 59));
        storeOrder(StoreOrderStatus.REQUESTED, null, LocalDateTime.of(2026, 9, 30, 23, 59, 59));
        storeOrder(StoreOrderStatus.REQUESTED, null, LocalDateTime.of(2026, 11, 1, 0, 0));
        storeOrder(StoreOrderStatus.COMPLETED, w1, LocalDateTime.of(2026, 10, 8, 9, 0));
        flush();

        Map<String, Long> byStatus = repository.countStoreOrdersByStatus(OCT);

        assertThat(byStatus).containsOnly(Map.entry("REQUESTED", 2L), Map.entry("COMPLETED", 1L));
    }

    @Test
    @DisplayName("창고별 비교는 활성 창고만, 창고 ID 순으로 재고·미달·입출고를 담는다")
    void warehouseRows() {
        seedInbounds();
        seedOutbounds();

        // 다른 테스트가 커밋해 둔 창고가 섞일 수 있어 이 테스트가 만든 창고만 본다
        List<WarehouseDashboardRow> rows = repository.findWarehouseRows(OCT).stream()
                .filter(row -> List.of(w1, w2, w3Inactive).contains(row.warehouseId()))
                .toList();

        assertThat(rows).extracting(WarehouseDashboardRow::warehouseId).containsExactly(w1, w2);
        WarehouseDashboardRow row1 = rows.get(0);
        WarehouseDashboardRow row2 = rows.get(1);
        // W1: 60(A1) + 20(B) + 0(E), 격리 로트 제외 / W2: 30(A1) + 20(D)
        assertThat(row1.inventoryQuantity()).isEqualTo(80L);
        assertThat(row2.inventoryQuantity()).isEqualTo(50L);
        // W1: A(가용 50 < 100), D(재고 없음 0 < 10) / W2: A(30 < 100), D는 20 >= 10
        assertThat(row1.lowStockSkuCount()).isEqualTo(2L);
        assertThat(row2.lowStockSkuCount()).isEqualTo(1L);
        assertThat(row1.inbound().completedQuantity()).isEqualTo(100L);
        assertThat(row1.inbound().inProgressQuantity()).isEqualTo(25L);
        assertThat(row2.inbound().completedQuantity()).isEqualTo(50L);
        assertThat(row2.inbound().inProgressQuantity()).isEqualTo(0L);
        assertThat(row1.outbound().completedQuantity()).isEqualTo(40L);
        assertThat(row1.outbound().inProgressQuantity()).isEqualTo(30L);
        assertThat(row2.outbound().completedQuantity()).isEqualTo(15L);
        assertThat(row2.outbound().inProgressQuantity()).isEqualTo(12L);
    }

    @Test
    @DisplayName("월별 완료 수량은 완료 건이 있는 달만 오름차순으로 담고 진행중 수량은 비운다")
    void monthlyCompleted() {
        seedInbounds();
        seedOutbounds();

        List<MonthlyFlow> flows = repository.findMonthlyCompleted(YearMonth.of(2026, 8), YearMonth.of(2026, 11), null);

        assertThat(flows).extracting(MonthlyFlow::month)
                .containsExactly(YearMonth.of(2026, 9), OCT, YearMonth.of(2026, 11));
        assertThat(flows).extracting(MonthlyFlow::inboundCompletedQuantity).containsExactly(70L, 150L, 0L);
        assertThat(flows).extracting(MonthlyFlow::outboundCompletedQuantity).containsExactly(25L, 55L, 99L);
        assertThat(flows).extracting(MonthlyFlow::inboundInProgressQuantity).containsOnlyNulls();
        assertThat(flows).extracting(MonthlyFlow::outboundInProgressQuantity).containsOnlyNulls();

        List<MonthlyFlow> w2Flows = repository.findMonthlyCompleted(YearMonth.of(2026, 8), YearMonth.of(2026, 11), w2);
        assertThat(w2Flows).extracting(MonthlyFlow::outboundCompletedQuantity).containsExactly(15L, 99L);
    }

    @Test
    @DisplayName("출고 상위 SKU는 해당 월 배송 완료 수량 내림차순이고 창고 필터를 따른다")
    void topOutboundSkus() {
        seedOutbounds();

        List<TopOutboundSku> top = repository.findTopOutboundSkus(OCT, null, 5);
        assertThat(top).extracting(TopOutboundSku::skuId).containsExactly(skuA, skuB);
        assertThat(top).extracting(TopOutboundSku::quantity).containsExactly(40L, 15L);
        assertThat(top.get(0).skuCode()).isEqualTo("SKU-A");

        assertThat(repository.findTopOutboundSkus(OCT, null, 1)).extracting(TopOutboundSku::skuId)
                .containsExactly(skuA);
        assertThat(repository.findTopOutboundSkus(OCT, w2, 5)).extracting(TopOutboundSku::skuId)
                .containsExactly(skuB);
    }

    // ---------- 시나리오 ----------

    private void seedInbounds() {
        inbound(InboundStatus.COMPLETED, w1, LocalDateTime.of(2026, 10, 2, 10, 0), 100L);
        inbound(InboundStatus.COMPLETED, w2, LocalDateTime.of(2026, 10, 15, 10, 0), 30L, 20L);
        inbound(InboundStatus.COMPLETED, w1, LocalDateTime.of(2026, 9, 10, 10, 0), 70L);
        inbound(InboundStatus.INSPECTING, w1, null, 25L);
        inbound(InboundStatus.ARRIVED, w1, null);
        inbound(InboundStatus.CANCELED, w1, null);
        flush();
    }

    private void seedOutbounds() {
        outbound(OutboundStatus.DELIVERED, w1, skuA, LocalDateTime.of(2026, 10, 10, 10, 0), 40L, 40L);
        outbound(OutboundStatus.DELIVERED, w1, skuA, LocalDateTime.of(2026, 9, 30, 23, 59, 59), 25L, 25L);
        outbound(OutboundStatus.DELIVERED, w2, skuB, LocalDateTime.of(2026, 10, 31, 23, 59, 59), 15L, 15L);
        outbound(OutboundStatus.DELIVERED, w2, skuB, LocalDateTime.of(2026, 11, 1, 0, 0), 99L, 99L);
        outbound(OutboundStatus.PICKING, w1, skuA, null, 30L, 0L);
        outbound(OutboundStatus.SHIPPED, w2, skuB, null, 12L, 12L);
        outbound(OutboundStatus.READY, w1, skuA, null, 77L, 0L);
        outbound(OutboundStatus.CANCELED, w1, skuA, null, 66L, 0L);
        flush();
    }

    // ---------- 픽스처 ----------

    private void flush() {
        em.flush();
        em.clear();
    }

    private Long warehouse(String code, WarehouseStatus status) {
        WarehouseJpaEntity entity = WarehouseJpaEntity.builder()
                .warehouseCode(code).name(code).address("주소").contactNumber("02-123-4567")
                .totalCapacity(BigDecimal.valueOf(1000)).status(status).build();
        em.persist(entity);
        return entity.getWarehouseId();
    }

    private Long section(Long warehouseId, String code) {
        WarehouseSectionJpaEntity entity = WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode(code + "-" + warehouseId).name(code).sectionType("STORAGE")
                .capacity(BigDecimal.valueOf(100)).currentCapacity(BigDecimal.ZERO)
                .status(WarehouseStatus.ACTIVE).build();
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

    private void inventoryLot(Long sectionId, Long lotId, Long onHand, Long allocated) {
        inventoryLot(sectionId, lotId, onHand, allocated, QualityStatus.AVAILABLE);
    }

    private void inventoryLot(Long sectionId, Long lotId, Long onHand, Long allocated, QualityStatus quality) {
        em.persist(InventoryLotJpaEntity.builder()
                .sectionId(sectionId).lotId(lotId).onHandQuantity(onHand).allocatedQuantity(allocated)
                .qualityStatus(quality).build());
    }

    private Long storeOrder(StoreOrderStatus status, Long warehouseId, LocalDateTime requestedAt) {
        StoreOrderJpaEntity entity = StoreOrderJpaEntity.builder()
                .orderNo("SO-" + (++seq)).storeId(1L).warehouseId(warehouseId).status(status)
                .requestedAt(requestedAt).createdBy(1L).build();
        em.persist(entity);
        return entity.getStoreOrderId();
    }

    private void purchaseOrder(PurchaseOrderStatus status) {
        em.persist(PurchaseOrderJpaEntity.builder()
                .purchaseOrderNo("PO-" + (++seq)).warehouseId(w1).status(status).createdBy(1L).build());
    }

    /** 검수 항목 수량마다 한 줄을 만든다. 수량이 없으면 항목이 없는 입고다. */
    private void inbound(InboundStatus status, Long warehouseId, LocalDateTime receivedAt, Long... lineQuantities) {
        InboundJpaEntity inbound = InboundJpaEntity.builder()
                .inboundNo("IB-" + (++seq)).purchaseOrderId(1L).warehouseId(warehouseId).status(status)
                .arrivedAt(LocalDateTime.of(2026, 9, 1, 9, 0)).receivedAt(receivedAt).build();
        em.persist(inbound);
        for (Long quantity : lineQuantities) {
            em.persist(InboundLineJpaEntity.builder()
                    .inboundId(inbound.getInboundId()).purchaseOrderLineId((long) (++seq)).lotId(1L)
                    .receivedQuantity(quantity).acceptedQuantity(quantity).defectiveQuantity(0L)
                    .receivedUnitPrice(BigDecimal.TEN).lineAmount(BigDecimal.TEN.multiply(BigDecimal.valueOf(quantity)))
                    .receivedAt(LocalDateTime.of(2026, 9, 1, 9, 0)).receivedBy(1L).build());
        }
    }

    /** 지점 발주(ASSIGNED) 한 건에 항목·할당·출고 한 줄을 만든다. 발주 요청 시각은 8월이라 월 통계에 섞이지 않는다. */
    private void outbound(OutboundStatus status, Long warehouseId, Long skuId, LocalDateTime deliveredAt,
                          Long allocatedQuantity, Long shippedQuantity) {
        Long storeOrderId = storeOrder(StoreOrderStatus.ASSIGNED, warehouseId, LocalDateTime.of(2026, 8, 1, 9, 0));
        StoreOrderLineJpaEntity line = StoreOrderLineJpaEntity.builder()
                .storeOrderId(storeOrderId).skuId(skuId).requestedQuantity(allocatedQuantity)
                .allocatedQuantity(0L).shippedQuantity(0L).requestedUnitSupplyPrice(BigDecimal.TEN)
                .status(StoreOrderLineStatus.REQUESTED).build();
        em.persist(line);
        StockAllocationJpaEntity allocation = StockAllocationJpaEntity.builder()
                .storeOrderLineId(line.getStoreOrderLineId()).inventoryLotId(1L)
                .allocatedQuantity(allocatedQuantity).pickedQuantity(shippedQuantity)
                .status(AllocationStatus.ALLOCATED).allocatedAt(LocalDateTime.of(2026, 8, 2, 9, 0))
                .allocatedBy(1L).build();
        em.persist(allocation);
        OutboundJpaEntity outbound = OutboundJpaEntity.builder()
                .outboundNo("OB-" + (++seq)).storeOrderId(storeOrderId).status(status)
                .deliveredAt(deliveredAt).build();
        em.persist(outbound);
        em.persist(OutboundLineJpaEntity.builder()
                .outboundId(outbound.getOutboundId()).allocationId(allocation.getAllocationId())
                .shippedQuantity(shippedQuantity).build());
    }
}

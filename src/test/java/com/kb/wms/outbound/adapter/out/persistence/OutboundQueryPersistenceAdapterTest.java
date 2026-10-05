package com.kb.wms.outbound.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.LotJpaRepository;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.outbound.application.port.in.query.OutboundSearchCondition;
import com.kb.wms.outbound.application.port.in.query.StockAllocationSearchCondition;
import com.kb.wms.outbound.application.port.in.result.FefoStockCandidate;
import com.kb.wms.outbound.application.port.in.result.OutboundLineView;
import com.kb.wms.outbound.application.port.in.result.OutboundSummary;
import com.kb.wms.outbound.application.port.in.result.OutboundView;
import com.kb.wms.outbound.application.port.in.result.StockAllocationSummary;
import com.kb.wms.outbound.application.port.in.result.StockAllocationView;
import com.kb.wms.outbound.application.port.out.OutboundQueryRepository;
import com.kb.wms.outbound.application.port.out.OutboundRepository;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.entity.OutboundLine;
import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.store.adapter.out.persistence.entity.StoreJpaEntity;
import com.kb.wms.store.adapter.out.persistence.repository.StoreJpaRepository;
import com.kb.wms.store.domain.enums.StoreStatus;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderJpaEntity;
import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderLineJpaEntity;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderJpaRepository;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderLineJpaRepository;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseSectionJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

/**
 * 출고·재고 할당 조회 쿼리(목록 필터·상세 조인·FEFO 후보) 검증.
 *
 * <p>데이터: 지점 1개, 창고 2개(서울/부산), 구역 3개(서울 2개 + 부산 1개), SKU 2개(A/B).
 * 서울 창고의 SKU-A 재고 행: 유통기한 없음, 유통기한 12-31, 유통기한 11-30(보유 10), 유통기한 11-30(전량 할당), 품질 불량.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class OutboundQueryPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 9, 0);

    @Autowired OutboundQueryRepository queryRepository;
    @Autowired OutboundRepository outboundRepository;
    @Autowired StockAllocationRepository allocationRepository;
    @Autowired StoreJpaRepository storeJpaRepository;
    @Autowired WarehouseJpaRepository warehouseJpaRepository;
    @Autowired WarehouseSectionJpaRepository sectionJpaRepository;
    @Autowired ProductSkuJpaRepository skuJpaRepository;
    @Autowired LotJpaRepository lotJpaRepository;
    @Autowired InventoryLotJpaRepository inventoryLotJpaRepository;
    @Autowired StoreOrderJpaRepository storeOrderJpaRepository;
    @Autowired StoreOrderLineJpaRepository storeOrderLineJpaRepository;

    Long store;
    Long seoul;
    Long busan;
    Long seoulSection1;
    Long seoulSection2;
    Long busanSection;
    Long skuA;
    Long skuB;
    Long order;
    Long otherOrder;
    Long lineA;
    Long lineB;
    Long otherLineA;
    Long lotNoExpiry;
    Long lotLate;
    Long lotEarly;
    Long lotEarlyFull;
    Long lotDefective;
    Long lotBusan;

    @BeforeEach
    void setUp() {
        store = storeJpaRepository.save(StoreJpaEntity.builder()
                .storeCode("ST-1").name("강남점").address("주소").contactNumber("02-1234-5678")
                .status(StoreStatus.ACTIVE).build()).getStoreId();
        seoul = warehouse("WH-SEOUL", "서울 물류센터");
        busan = warehouse("WH-BUSAN", "부산 물류센터");
        seoulSection1 = section(seoul, "A-01", WarehouseStatus.ACTIVE);
        seoulSection2 = section(seoul, "A-02", WarehouseStatus.ACTIVE);
        busanSection = section(busan, "B-01", WarehouseStatus.ACTIVE);
        skuA = sku("SKU-A", "에이 상품");
        skuB = sku("SKU-B", "비 상품");

        lotNoExpiry = inventory(seoulSection1, skuA, "LOT-N", null, 0, 20, 0, QualityStatus.AVAILABLE, LotStatus.AVAILABLE);
        lotLate = inventory(seoulSection1, skuA, "LOT-L", LocalDate.of(2026, 12, 31), 1, 20, 0, QualityStatus.AVAILABLE, LotStatus.AVAILABLE);
        lotEarly = inventory(seoulSection2, skuA, "LOT-E", LocalDate.of(2026, 11, 30), 2, 10, 4, QualityStatus.AVAILABLE, LotStatus.AVAILABLE);
        lotEarlyFull = inventory(seoulSection2, skuA, "LOT-F", LocalDate.of(2026, 11, 30), 3, 5, 5, QualityStatus.AVAILABLE, LotStatus.AVAILABLE);
        lotDefective = inventory(seoulSection1, skuA, "LOT-D", LocalDate.of(2026, 10, 31), 4, 20, 0, QualityStatus.DEFECTIVE, LotStatus.AVAILABLE);
        lotBusan = inventory(busanSection, skuA, "LOT-B", LocalDate.of(2026, 10, 15), 5, 20, 0, QualityStatus.AVAILABLE, LotStatus.AVAILABLE);

        order = order("SO-20261005-0001", seoul);
        otherOrder = order("SO-20261005-0002", busan);
        lineA = line(order, skuA, 50);
        lineB = line(order, skuB, 5);
        otherLineA = line(otherOrder, skuA, 7);
    }

    private Long warehouse(String code, String name) {
        return warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode(code).name(name).address("주소").totalCapacity(BigDecimal.valueOf(1000))
                .status(WarehouseStatus.ACTIVE).build()).getWarehouseId();
    }

    private Long section(Long warehouseId, String code, WarehouseStatus status) {
        return sectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode(code).name(code + " 구역").sectionType("STORAGE")
                .capacity(BigDecimal.valueOf(1000)).currentCapacity(BigDecimal.ZERO).status(status)
                .build()).getSectionId();
    }

    private Long sku(String code, String name) {
        return skuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode(code).name(name).unit("EA")
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build()).getSkuId();
    }

    /** 로트 1개와 재고 행 1개를 만든다. 로트 생성 순서는 seq 순서다. */
    private Long inventory(Long sectionId, Long skuId, String lotNumber, LocalDate expiry, int seq,
                           long onHand, long allocated, QualityStatus quality, LotStatus lotStatus) {
        Long lotId = lotJpaRepository.save(LotJpaEntity.builder()
                .skuId(skuId).supplierId(1L).lotNumber(lotNumber).expiryDate(expiry)
                .status(lotStatus).unitCost(BigDecimal.TEN).build()).getLotId();
        return inventoryLotJpaRepository.save(InventoryLotJpaEntity.builder()
                .sectionId(sectionId).lotId(lotId).onHandQuantity(onHand).allocatedQuantity(allocated)
                .qualityStatus(quality).build()).getInventoryLotId();
    }

    private Long order(String no, Long warehouseId) {
        return storeOrderJpaRepository.save(StoreOrderJpaEntity.builder()
                .orderNo(no).storeId(store).warehouseId(warehouseId).status(StoreOrderStatus.ASSIGNED)
                .requestedAt(NOW).createdBy(1L).build()).getStoreOrderId();
    }

    private Long line(Long orderId, Long skuId, long requested) {
        return storeOrderLineJpaRepository.save(StoreOrderLineJpaEntity.builder()
                .storeOrderId(orderId).skuId(skuId).requestedQuantity(requested).allocatedQuantity(0L)
                .shippedQuantity(0L).requestedUnitSupplyPrice(new BigDecimal("1000"))
                .status(StoreOrderLineStatus.REQUESTED).build()).getStoreOrderLineId();
    }

    private StockAllocation allocation(Long lineId, Long inventoryLotId, long quantity, LocalDateTime at) {
        return allocationRepository.save(StockAllocation.allocate(lineId, inventoryLotId, quantity, 1L, at));
    }

    private Outbound outbound(String no, Long orderId, Long... allocationIds) {
        Outbound saved = outboundRepository.save(Outbound.create(no, orderId, "메모"));
        outboundRepository.saveLines(java.util.Arrays.stream(allocationIds)
                .map(id -> OutboundLine.create(saved.getOutboundId(), id)).toList());
        return saved;
    }

    // ---------- FEFO ----------

    @Test
    @DisplayName("FEFO 후보는 가용 행만 유통기한 빠른 순(없으면 뒤)으로, 가용 수량과 함께 돌려준다")
    void fefoCandidates() {
        List<FefoStockCandidate> candidates = queryRepository.findFefoCandidates(seoul, Set.of(skuA));

        assertThat(candidates).extracting(FefoStockCandidate::inventoryLotId)
                .containsExactly(lotEarly, lotLate, lotNoExpiry);
        assertThat(candidates.get(0).availableQuantity()).isEqualTo(6);
        assertThat(candidates.get(0).skuId()).isEqualTo(skuA);
        assertThat(candidates.get(0).sectionId()).isEqualTo(seoulSection2);
    }

    @Test
    @DisplayName("FEFO 후보는 다른 창고·불량 품질·전량 할당 행을 제외하고 SKU가 없으면 비어 있다")
    void fefoExcludes() {
        assertThat(queryRepository.findFefoCandidates(seoul, Set.of(skuA)))
                .extracting(FefoStockCandidate::inventoryLotId)
                .doesNotContain(lotBusan, lotDefective, lotEarlyFull);
        assertThat(queryRepository.findFefoCandidates(seoul, Set.of(skuB))).isEmpty();
        assertThat(queryRepository.findFefoCandidates(seoul, Set.of())).isEmpty();
    }

    @Test
    @DisplayName("비활성 구역과 가용하지 않은 로트는 후보에서 제외된다")
    void fefoExcludesInactiveSectionAndLot() {
        Long inactive = section(seoul, "A-99", WarehouseStatus.INACTIVE);
        Long inInactive = inventory(inactive, skuA, "LOT-I", LocalDate.of(2026, 10, 1), 6, 10, 0,
                QualityStatus.AVAILABLE, LotStatus.AVAILABLE);
        Long quarantined = inventory(seoulSection1, skuA, "LOT-Q", LocalDate.of(2026, 10, 1), 7, 10, 0,
                QualityStatus.AVAILABLE, LotStatus.QUARANTINED);

        assertThat(queryRepository.findFefoCandidates(seoul, Set.of(skuA)))
                .extracting(FefoStockCandidate::inventoryLotId)
                .doesNotContain(inInactive, quarantined);
    }

    // ---------- 재고 할당 조회 ----------

    @Test
    @DisplayName("할당 목록은 할당 일시 내림차순이고 발주·SKU·로트·구역 정보를 담는다")
    void searchAllocations() {
        StockAllocation older = allocation(lineA, lotEarly, 5, NOW);
        StockAllocation newer = allocation(lineA, lotLate, 3, NOW.plusHours(1));
        allocation(otherLineA, lotBusan, 2, NOW.plusHours(2));

        List<StockAllocationSummary> items = queryRepository.searchAllocations(
                new StockAllocationSearchCondition(order, null, null, null, null));

        assertThat(items).extracting(StockAllocationSummary::allocationId)
                .containsExactly(newer.getAllocationId(), older.getAllocationId());
        StockAllocationSummary first = items.get(1);
        assertThat(first.orderNo()).isEqualTo("SO-20261005-0001");
        assertThat(first.skuCode()).isEqualTo("SKU-A");
        assertThat(first.lotNumber()).isEqualTo("LOT-E");
        assertThat(first.expiryDate()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(first.sectionCode()).isEqualTo("A-02");
        assertThat(first.warehouseId()).isEqualTo(seoul);
        assertThat(first.status()).isEqualTo(AllocationStatus.ALLOCATED);
    }

    @Test
    @DisplayName("할당 목록 필터: 창고·SKU·상태·키워드")
    void searchAllocationsFilters() {
        StockAllocation a = allocation(lineA, lotEarly, 5, NOW);
        StockAllocation released = allocation(lineA, lotLate, 3, NOW.plusHours(1));
        released.release(NOW.plusHours(2));
        allocationRepository.save(released);
        StockAllocation busanAllocation = allocation(otherLineA, lotBusan, 2, NOW.plusHours(3));

        assertThat(ids(new StockAllocationSearchCondition(null, busan, null, null, null)))
                .containsExactly(busanAllocation.getAllocationId());
        assertThat(ids(new StockAllocationSearchCondition(null, seoul, skuA, AllocationStatus.ALLOCATED, null)))
                .containsExactly(a.getAllocationId());
        assertThat(ids(new StockAllocationSearchCondition(null, null, null, AllocationStatus.RELEASED, null)))
                .containsExactly(released.getAllocationId());
        assertThat(ids(new StockAllocationSearchCondition(null, null, null, null, "lot-l")))
                .containsExactly(released.getAllocationId());
        assertThat(ids(new StockAllocationSearchCondition(null, null, null, null, "SO-20261005-0002")))
                .containsExactly(busanAllocation.getAllocationId());
        assertThat(ids(new StockAllocationSearchCondition(null, null, skuB, null, null))).isEmpty();
        assertThat(ids(new StockAllocationSearchCondition(null, null, null, null, "  "))).hasSize(3);
    }

    private List<Long> ids(StockAllocationSearchCondition condition) {
        return queryRepository.searchAllocations(condition).stream()
                .map(StockAllocationSummary::allocationId).toList();
    }

    @Test
    @DisplayName("할당 ID 목록 요약은 ID 오름차순이고 빈 목록이면 빈 결과")
    void allocationSummariesByIds() {
        StockAllocation a1 = allocation(lineA, lotEarly, 5, NOW);
        StockAllocation a2 = allocation(lineA, lotLate, 3, NOW.plusHours(1));

        assertThat(queryRepository.findAllocationSummaries(Set.of(a2.getAllocationId(), a1.getAllocationId())))
                .extracting(StockAllocationSummary::allocationId)
                .containsExactly(a1.getAllocationId(), a2.getAllocationId());
        assertThat(queryRepository.findAllocationSummaries(Set.of())).isEmpty();
    }

    @Test
    @DisplayName("할당 상세에 지점·발주 항목·구역 정보가 담기고 연결된 출고는 취소되지 않은 것만 찾는다")
    void allocationView() {
        StockAllocation linked = allocation(lineA, lotEarly, 5, NOW);
        StockAllocation inCanceled = allocation(lineA, lotLate, 3, NOW);
        StockAllocation free = allocation(lineA, lotNoExpiry, 2, NOW);
        Outbound active = outbound("OB-20261005-0001", order, linked.getAllocationId());
        Outbound canceled = outbound("OB-20261005-0002", order, inCanceled.getAllocationId());
        canceled.cancel();
        outboundRepository.save(canceled);

        StockAllocationView view = queryRepository.findAllocationView(linked.getAllocationId()).orElseThrow();

        assertThat(view.storeName()).isEqualTo("강남점");
        assertThat(view.requestedQuantity()).isEqualTo(50);
        assertThat(view.skuName()).isEqualTo("에이 상품");
        assertThat(view.sectionName()).isEqualTo("A-02 구역");
        assertThat(view.allocatedBy()).isEqualTo(1L);
        assertThat(queryRepository.findActiveOutboundIdByAllocationId(linked.getAllocationId()))
                .contains(active.getOutboundId());
        assertThat(queryRepository.findActiveOutboundIdByAllocationId(inCanceled.getAllocationId())).isEmpty();
        assertThat(queryRepository.findActiveOutboundIdByAllocationId(free.getAllocationId())).isEmpty();
        assertThat(queryRepository.findAllocationView(999_999L)).isEmpty();
    }

    // ---------- 출고 조회 ----------

    @Test
    @DisplayName("출고 목록은 생성 일시 내림차순이고 항목 수와 창고·지점 이름을 담는다")
    void searchOutbounds() {
        StockAllocation a1 = allocation(lineA, lotEarly, 5, NOW);
        StockAllocation a2 = allocation(lineA, lotLate, 3, NOW);
        StockAllocation a3 = allocation(otherLineA, lotBusan, 2, NOW);
        Outbound first = outbound("OB-20261005-0001", order, a1.getAllocationId(), a2.getAllocationId());
        Outbound second = outbound("OB-20261005-0002", otherOrder, a3.getAllocationId());

        List<OutboundSummary> items = queryRepository.searchOutbounds(
                new OutboundSearchCondition(null, null, null, null, null, null, null));

        assertThat(items).extracting(OutboundSummary::outboundId)
                .containsExactly(second.getOutboundId(), first.getOutboundId());
        OutboundSummary firstSummary = items.get(1);
        assertThat(firstSummary.lineCount()).isEqualTo(2);
        assertThat(firstSummary.warehouseName()).isEqualTo("서울 물류센터");
        assertThat(firstSummary.storeName()).isEqualTo("강남점");
        assertThat(firstSummary.orderNo()).isEqualTo("SO-20261005-0001");
        assertThat(firstSummary.status()).isEqualTo(OutboundStatus.READY);
    }

    @Test
    @DisplayName("출고 목록 필터: 상태·창고·발주·키워드·기간")
    void searchOutboundsFilters() {
        StockAllocation a1 = allocation(lineA, lotEarly, 5, NOW);
        StockAllocation a3 = allocation(otherLineA, lotBusan, 2, NOW);
        Outbound seoulOutbound = outbound("OB-20261005-0001", order, a1.getAllocationId());
        Outbound busanOutbound = outbound("OB-20261005-0002", otherOrder, a3.getAllocationId());
        busanOutbound.startPicking();
        outboundRepository.save(busanOutbound);

        assertThat(outboundIds(new OutboundSearchCondition(OutboundStatus.PICKING, null, null, null, null, null, null)))
                .containsExactly(busanOutbound.getOutboundId());
        assertThat(outboundIds(new OutboundSearchCondition(null, seoul, null, null, null, null, null)))
                .containsExactly(seoulOutbound.getOutboundId());
        assertThat(outboundIds(new OutboundSearchCondition(null, null, store, order, null, null, null)))
                .containsExactly(seoulOutbound.getOutboundId());
        assertThat(outboundIds(new OutboundSearchCondition(null, null, null, null, "ob-20261005-0002", null, null)))
                .containsExactly(busanOutbound.getOutboundId());
        assertThat(outboundIds(new OutboundSearchCondition(null, null, null, null, "SO-20261005-0001", null, null)))
                .containsExactly(seoulOutbound.getOutboundId());
        assertThat(outboundIds(new OutboundSearchCondition(null, null, null, null, null,
                LocalDateTime.now().plusDays(1), null))).isEmpty();
        assertThat(outboundIds(new OutboundSearchCondition(null, null, null, null, null,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)))).hasSize(2);
    }

    private List<Long> outboundIds(OutboundSearchCondition condition) {
        return queryRepository.searchOutbounds(condition).stream().map(OutboundSummary::outboundId).toList();
    }

    @Test
    @DisplayName("출고 상세 헤더와 항목은 피킹 위치·할당 수량을 담고 금액은 피킹 전 null이다")
    void outboundDetails() {
        StockAllocation a1 = allocation(lineA, lotEarly, 5, NOW);
        StockAllocation a2 = allocation(lineA, lotLate, 3, NOW);
        Outbound saved = outbound("OB-20261005-0001", order, a1.getAllocationId(), a2.getAllocationId());

        OutboundView view = queryRepository.findOutboundView(saved.getOutboundId()).orElseThrow();
        List<OutboundLineView> lines = queryRepository.findOutboundLineViews(saved.getOutboundId());

        assertThat(view.outboundNo()).isEqualTo("OB-20261005-0001");
        assertThat(view.note()).isEqualTo("메모");
        assertThat(view.warehouseName()).isEqualTo("서울 물류센터");
        assertThat(view.deliveredAt()).isNull();
        assertThat(lines).extracting(OutboundLineView::allocationId)
                .containsExactly(a1.getAllocationId(), a2.getAllocationId());
        OutboundLineView line = lines.get(0);
        assertThat(line.skuCode()).isEqualTo("SKU-A");
        assertThat(line.unit()).isEqualTo("EA");
        assertThat(line.lotNumber()).isEqualTo("LOT-E");
        assertThat(line.sectionCode()).isEqualTo("A-02");
        assertThat(line.allocatedQuantity()).isEqualTo(5);
        assertThat(line.shippedQuantity()).isZero();
        assertThat(line.confirmedUnitSupplyPrice()).isNull();
        assertThat(line.lineAmount()).isNull();
        assertThat(queryRepository.findOutboundView(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("피킹 확정 후 항목 금액은 출고 수량 × 확정 공급 단가다")
    void outboundLineAmount() {
        StockAllocation a = allocation(lineA, lotEarly, 5, NOW);
        Outbound saved = outbound("OB-20261005-0001", order, a.getAllocationId());
        OutboundLine line = outboundRepository.findLinesByOutboundId(saved.getOutboundId()).get(0);
        line.confirmPicking(4, 5, new BigDecimal("1500.00"));
        outboundRepository.saveLines(List.of(line));

        OutboundLineView view = queryRepository.findOutboundLineViews(saved.getOutboundId()).get(0);

        assertThat(view.shippedQuantity()).isEqualTo(4);
        assertThat(view.lineAmount()).isEqualByComparingTo("6000.00");
    }
}

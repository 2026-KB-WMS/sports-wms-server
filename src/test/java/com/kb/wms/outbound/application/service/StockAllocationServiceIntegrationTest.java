package com.kb.wms.outbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.entity.StatusHistory;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.LotJpaRepository;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.outbound.application.port.in.StockAllocationUseCase;
import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;
import com.kb.wms.outbound.application.port.in.command.StockAllocationReleaseCommand;
import com.kb.wms.outbound.application.port.in.query.StockAllocationSearchCondition;
import com.kb.wms.outbound.application.port.in.result.StockAllocateResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationDetail;
import com.kb.wms.outbound.application.port.in.result.StockAllocationReleaseResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationSummary;
import com.kb.wms.outbound.application.port.out.OutboundRepository;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.entity.OutboundLine;
import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
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

import jakarta.persistence.EntityManager;

/**
 * 재고 할당·해제 서비스를 실제 재고 서비스·발주 연동·상태 이력과 함께 검증한다.
 *
 * <p>데이터: 서울 창고 구역 2개, SKU-A 재고 3행(유통기한 11-30 보유 10/할당 4 → 가용 6, 12-31 가용 20, 유통기한 없음 가용 20),
 * SKU-B 재고 없음. 발주 항목 A(요청 12)·B(요청 5)가 있는 배정(ASSIGNED) 발주.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class StockAllocationServiceIntegrationTest {

    private static final Long USER = 7L;

    @Autowired StockAllocationUseCase useCase;
    @Autowired StockAllocationRepository allocationRepository;
    @Autowired OutboundRepository outboundRepository;
    @Autowired StatusHistoryUseCase statusHistoryUseCase;
    @Autowired StoreJpaRepository storeJpaRepository;
    @Autowired WarehouseJpaRepository warehouseJpaRepository;
    @Autowired WarehouseSectionJpaRepository sectionJpaRepository;
    @Autowired ProductSkuJpaRepository skuJpaRepository;
    @Autowired LotJpaRepository lotJpaRepository;
    @Autowired InventoryLotJpaRepository inventoryLotJpaRepository;
    @Autowired StoreOrderJpaRepository storeOrderJpaRepository;
    @Autowired StoreOrderLineJpaRepository storeOrderLineJpaRepository;
    @Autowired EntityManager entityManager;

    Long store;
    Long warehouse;
    Long skuA;
    Long skuB;
    Long lotEarly;
    Long lotLate;
    Long lotNoExpiry;
    Long order;
    Long lineA;
    Long lineB;

    @BeforeEach
    void setUp() {
        store = storeJpaRepository.save(StoreJpaEntity.builder()
                .storeCode("ST-1").name("강남점").address("주소").contactNumber("02-1234-5678")
                .status(StoreStatus.ACTIVE).build()).getStoreId();
        warehouse = warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode("WH-1").name("서울 물류센터").address("주소").totalCapacity(BigDecimal.valueOf(1000))
                .status(WarehouseStatus.ACTIVE).build()).getWarehouseId();
        Long section1 = section("A-01");
        Long section2 = section("A-02");
        skuA = sku("SKU-A");
        skuB = sku("SKU-B");
        lotEarly = inventory(section2, skuA, "LOT-E", LocalDate.of(2026, 11, 30), 10, 4);
        lotLate = inventory(section1, skuA, "LOT-L", LocalDate.of(2026, 12, 31), 20, 0);
        lotNoExpiry = inventory(section1, skuA, "LOT-N", null, 20, 0);

        order = storeOrderJpaRepository.save(StoreOrderJpaEntity.builder()
                .orderNo("SO-20261005-0001").storeId(store).warehouseId(warehouse)
                .status(StoreOrderStatus.ASSIGNED).requestedAt(LocalDateTime.of(2026, 10, 5, 9, 0))
                .createdBy(1L).build()).getStoreOrderId();
        lineA = line(order, skuA, 12);
        lineB = line(order, skuB, 5);
    }

    private Long section(String code) {
        return sectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouse).sectionCode(code).name(code).sectionType("STORAGE")
                .capacity(BigDecimal.valueOf(1000)).currentCapacity(BigDecimal.ZERO)
                .status(WarehouseStatus.ACTIVE).build()).getSectionId();
    }

    private Long sku(String code) {
        return skuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode(code).name(code).unit("EA")
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build()).getSkuId();
    }

    private Long inventory(Long sectionId, Long skuId, String lotNumber, LocalDate expiry, long onHand, long allocated) {
        Long lotId = lotJpaRepository.save(LotJpaEntity.builder()
                .skuId(skuId).supplierId(1L).lotNumber(lotNumber).expiryDate(expiry)
                .status(LotStatus.AVAILABLE).unitCost(BigDecimal.TEN).build()).getLotId();
        return inventoryLotJpaRepository.save(InventoryLotJpaEntity.builder()
                .sectionId(sectionId).lotId(lotId).onHandQuantity(onHand).allocatedQuantity(allocated)
                .qualityStatus(QualityStatus.AVAILABLE).build()).getInventoryLotId();
    }

    private Long line(Long orderId, Long skuId, long requested) {
        return storeOrderLineJpaRepository.save(StoreOrderLineJpaEntity.builder()
                .storeOrderId(orderId).skuId(skuId).requestedQuantity(requested).allocatedQuantity(0L)
                .shippedQuantity(0L).requestedUnitSupplyPrice(new BigDecimal("1000"))
                .status(StoreOrderLineStatus.REQUESTED).build()).getStoreOrderLineId();
    }

    /** SKU-B 재고를 만들어 전체 할당이 가능하게 한다. */
    private void stockSkuB() {
        inventory(section("B-01"), skuB, "LOT-B", LocalDate.of(2026, 12, 1), 10, 0);
    }

    private long inventoryAllocated(Long inventoryLotId) {
        entityManager.flush();
        entityManager.clear();
        return inventoryLotJpaRepository.findById(inventoryLotId).orElseThrow().getAllocatedQuantity();
    }

    private long inventoryOnHand(Long inventoryLotId) {
        return inventoryLotJpaRepository.findById(inventoryLotId).orElseThrow().getOnHandQuantity();
    }

    private long lineAllocated(Long lineId) {
        entityManager.flush();
        entityManager.clear();
        return storeOrderLineJpaRepository.findById(lineId).orElseThrow().getAllocatedQuantity();
    }

    private String errorCodeOf(Throwable t) {
        return ((BusinessException) t).getErrorCodeName();
    }

    // ---------- 할당 ----------

    @Test
    @DisplayName("FEFO 순서로 재고 행에 나눠 할당하고 재고·발주 항목 할당 수량과 이력을 남긴다(보유 수량 불변)")
    void allocateSplitsByFefo() {
        stockSkuB();

        StockAllocateResult result = useCase.allocate(new StockAllocateCommand(order, USER));

        assertThat(result.orderNo()).isEqualTo("SO-20261005-0001");
        List<StockAllocationSummary> itemsOfA = result.items().stream()
                .filter(i -> i.skuId().equals(skuA)).toList();
        assertThat(itemsOfA).extracting(StockAllocationSummary::inventoryLotId).containsExactly(lotEarly, lotLate);
        assertThat(itemsOfA).extracting(StockAllocationSummary::allocatedQuantity).containsExactly(6L, 6L);
        assertThat(result.items()).hasSize(3).allSatisfy(i -> {
            assertThat(i.status()).isEqualTo(AllocationStatus.ALLOCATED);
            assertThat(i.pickedQuantity()).isZero();
        });

        assertThat(inventoryAllocated(lotEarly)).isEqualTo(10);
        assertThat(inventoryAllocated(lotLate)).isEqualTo(6);
        assertThat(inventoryAllocated(lotNoExpiry)).isZero();
        assertThat(inventoryOnHand(lotEarly)).isEqualTo(10);
        assertThat(lineAllocated(lineA)).isEqualTo(12);
        assertThat(lineAllocated(lineB)).isEqualTo(5);

        Long firstId = itemsOfA.get(0).allocationId();
        List<StatusHistory> history = statusHistoryUseCase.findHistory(StatusHistoryEntityType.STOCK_ALLOCATION, firstId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getFromStatus()).isNull();
        assertThat(history.get(0).getToStatus()).isEqualTo("ALLOCATED");
        assertThat(history.get(0).getChangedBy()).isEqualTo(USER);
    }

    @Test
    @DisplayName("한 항목이라도 가용 재고가 부족하면 아무것도 할당하지 않고 INSUFFICIENT_STOCK")
    void allocateAllOrNothing() {
        assertThatThrownBy(() -> useCase.allocate(new StockAllocateCommand(order, USER)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    assertThat(errorCodeOf(e)).isEqualTo("INSUFFICIENT_STOCK");
                    assertThat(e.getMessage()).contains("skuId=" + skuB);
                });

        assertThat(allocationRepository.findByStoreOrderId(order)).isEmpty();
        assertThat(inventoryAllocated(lotLate)).isZero();
        assertThat(lineAllocated(lineA)).isZero();
    }

    @Test
    @DisplayName("재고가 모자라면 가용 수량을 메시지에 담는다")
    void allocateShortageMessage() {
        stockSkuB();
        entityManager.flush();
        // SKU-A 가용은 46인데 요청 100으로 키운다
        StoreOrderLineJpaEntity big = storeOrderLineJpaRepository.findById(lineA).orElseThrow();
        storeOrderLineJpaRepository.delete(big);
        entityManager.flush();
        line(order, skuA, 100);

        assertThatThrownBy(() -> useCase.allocate(new StockAllocateCommand(order, USER)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("요청 100, 가용 46");
    }

    @Test
    @DisplayName("이미 전부 할당한 발주를 다시 할당하면 ALREADY_ALLOCATED")
    void allocateTwice() {
        stockSkuB();
        useCase.allocate(new StockAllocateCommand(order, USER));

        assertThatThrownBy(() -> useCase.allocate(new StockAllocateCommand(order, USER)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("ALREADY_ALLOCATED"));
    }

    @Test
    @DisplayName("ASSIGNED 가 아닌 발주는 할당할 수 없다(CONFLICT)")
    void allocateNotAssigned() {
        StoreOrderJpaEntity hold = storeOrderJpaRepository.findById(order).orElseThrow();
        storeOrderJpaRepository.save(StoreOrderJpaEntity.builder()
                .storeOrderId(order).orderNo(hold.getOrderNo()).storeId(store).warehouseId(warehouse)
                .status(StoreOrderStatus.ON_HOLD).requestedAt(hold.getRequestedAt()).createdBy(1L).build());

        assertThatThrownBy(() -> useCase.allocate(new StockAllocateCommand(order, USER)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("CONFLICT"));
    }

    @Test
    @DisplayName("없는 발주와 사용자 누락은 각각 404, 400")
    void allocateInvalid() {
        assertThatThrownBy(() -> useCase.allocate(new StockAllocateCommand(999_999L, USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("STORE_ORDER_NOT_FOUND"));
        assertThatThrownBy(() -> useCase.allocate(new StockAllocateCommand(order, null)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.allocate(new StockAllocateCommand(null, USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
    }

    // ---------- 해제 ----------

    @Test
    @DisplayName("할당을 해제하면 RELEASED 가 되고 재고·발주 항목 할당 수량이 줄며 사유가 이력에 남는다")
    void release() {
        stockSkuB();
        StockAllocateResult allocated = useCase.allocate(new StockAllocateCommand(order, USER));
        StockAllocationSummary target = allocated.items().stream()
                .filter(i -> i.inventoryLotId().equals(lotLate)).findFirst().orElseThrow();

        StockAllocationReleaseResult result = useCase.release(
                new StockAllocationReleaseCommand(target.allocationId(), "  재고 재배치  ", USER));

        assertThat(result.status()).isEqualTo(AllocationStatus.RELEASED);
        assertThat(result.releasedAt()).isNotNull();
        assertThat(result.inventoryLotId()).isEqualTo(lotLate);
        assertThat(result.onHandQuantity()).isEqualTo(20);
        assertThat(result.inventoryAllocatedQuantity()).isZero();
        assertThat(result.availableQuantity()).isEqualTo(20);
        assertThat(inventoryAllocated(lotLate)).isZero();
        assertThat(lineAllocated(lineA)).isEqualTo(6);
        List<StatusHistory> history = statusHistoryUseCase
                .findHistory(StatusHistoryEntityType.STOCK_ALLOCATION, target.allocationId());
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getFromStatus()).isEqualTo("ALLOCATED");
        assertThat(history.get(1).getToStatus()).isEqualTo("RELEASED");
        assertThat(history.get(1).getReason()).isEqualTo("재고 재배치");
    }

    @Test
    @DisplayName("해제 후 남은 수량은 다시 할당할 수 있다")
    void allocateAfterRelease() {
        stockSkuB();
        StockAllocateResult allocated = useCase.allocate(new StockAllocateCommand(order, USER));
        Long id = allocated.items().get(0).allocationId();
        useCase.release(new StockAllocationReleaseCommand(id, "재할당", USER));

        StockAllocateResult again = useCase.allocate(new StockAllocateCommand(order, USER));

        assertThat(again.items()).hasSize(1);
        assertThat(again.items().get(0).allocatedQuantity())
                .isEqualTo(allocated.items().get(0).allocatedQuantity());
    }

    @Test
    @DisplayName("이미 해제됐거나 피킹된 할당은 해제할 수 없다(CONFLICT)")
    void releaseTerminal() {
        stockSkuB();
        StockAllocateResult allocated = useCase.allocate(new StockAllocateCommand(order, USER));
        Long released = allocated.items().get(0).allocationId();
        useCase.release(new StockAllocationReleaseCommand(released, "사유", USER));

        assertThatThrownBy(() -> useCase.release(new StockAllocationReleaseCommand(released, "사유", USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("CONFLICT"));
    }

    @Test
    @DisplayName("취소되지 않은 출고에 연결된 할당은 해제할 수 없고 취소된 뒤에는 해제할 수 있다")
    void releaseInOutbound() {
        stockSkuB();
        StockAllocateResult allocated = useCase.allocate(new StockAllocateCommand(order, USER));
        Long id = allocated.items().get(0).allocationId();
        Outbound outbound = outboundRepository.save(Outbound.create("OB-20261005-0001", order, null));
        outboundRepository.saveLines(List.of(OutboundLine.create(outbound.getOutboundId(), id)));

        assertThatThrownBy(() -> useCase.release(new StockAllocationReleaseCommand(id, "사유", USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("ALLOCATION_IN_OUTBOUND"));

        outbound.cancel();
        outboundRepository.save(outbound);
        StockAllocationReleaseResult result = useCase.release(new StockAllocationReleaseCommand(id, "사유", USER));
        assertThat(result.status()).isEqualTo(AllocationStatus.RELEASED);
    }

    @Test
    @DisplayName("해제 입력 검증: 사유 필수·500자 이하, 사용자 필수, 없는 할당은 404")
    void releaseInvalid() {
        stockSkuB();
        Long id = useCase.allocate(new StockAllocateCommand(order, USER)).items().get(0).allocationId();

        assertThatThrownBy(() -> useCase.release(new StockAllocationReleaseCommand(id, "  ", USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.release(new StockAllocationReleaseCommand(id, null, USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.release(new StockAllocationReleaseCommand(id, "가".repeat(501), USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.release(new StockAllocationReleaseCommand(id, "사유", null)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.release(new StockAllocationReleaseCommand(999_999L, "사유", USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("ALLOCATION_NOT_FOUND"));
    }

    // ---------- 조회 ----------

    @Test
    @DisplayName("상세 조회는 연결된 출고 ID를 담고 없는 할당은 404")
    void getAllocation() {
        stockSkuB();
        StockAllocateResult allocated = useCase.allocate(new StockAllocateCommand(order, USER));
        Long linked = allocated.items().get(0).allocationId();
        Long free = allocated.items().get(1).allocationId();
        Outbound outbound = outboundRepository.save(Outbound.create("OB-20261005-0001", order, null));
        outboundRepository.saveLines(List.of(OutboundLine.create(outbound.getOutboundId(), linked)));

        StockAllocationDetail linkedDetail = useCase.getAllocation(linked);
        StockAllocationDetail freeDetail = useCase.getAllocation(free);

        assertThat(linkedDetail.outboundId()).isEqualTo(outbound.getOutboundId());
        assertThat(linkedDetail.view().storeName()).isEqualTo("강남점");
        assertThat(freeDetail.outboundId()).isNull();
        assertThatThrownBy(() -> useCase.getAllocation(999_999L))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("ALLOCATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("목록 조회는 발주·상태 필터를 적용한다")
    void searchAllocations() {
        stockSkuB();
        useCase.allocate(new StockAllocateCommand(order, USER));

        List<StockAllocationSummary> all = useCase.searchAllocations(
                new StockAllocationSearchCondition(order, null, null, null, null));
        List<StockAllocationSummary> released = useCase.searchAllocations(
                new StockAllocationSearchCondition(order, null, null, AllocationStatus.RELEASED, null));

        assertThat(all).hasSize(3);
        assertThat(released).isEmpty();
    }
}

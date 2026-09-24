package com.kb.wms.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.inventory.application.port.in.InventoryAdjustmentUseCase;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.InventoryStockUseCase;
import com.kb.wms.inventory.application.port.in.LotUseCase;
import com.kb.wms.inventory.application.port.in.command.InventoryAdjustCommand;
import com.kb.wms.inventory.application.port.in.command.LotRegisterCommand;
import com.kb.wms.inventory.application.port.in.command.StockQuantityCommand;
import com.kb.wms.inventory.application.port.in.command.StockReceiveCommand;
import com.kb.wms.inventory.application.port.in.command.StockShipCommand;
import com.kb.wms.inventory.application.port.in.query.InventoryTransactionSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryAdjustmentResult;
import com.kb.wms.inventory.application.port.in.result.InventoryTransactionView;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.inventory.domain.enums.TransactionType;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseSectionJpaRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import jakarta.persistence.EntityManager;

/**
 * 재고 서비스 흐름 통합 검증 (실제 잠금 조회·구역 용량 증감·이력 기록).
 * 기본 데이터: 구역 R1(수용량 200, 사용 100), LOT-A 재고 100(할당 20).
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class InventoryServiceIntegrationTest {

    private static final long USER = 5L;

    @Autowired private InventoryAdjustmentUseCase adjustmentUseCase;
    @Autowired private InventoryStockUseCase stockUseCase;
    @Autowired private InventoryQueryUseCase queryUseCase;
    @Autowired private LotUseCase lotUseCase;
    @Autowired private InventoryLotRepository inventoryLotRepository;
    @Autowired private LotRepository lotRepository;
    @Autowired private WarehouseSectionRepository warehouseSectionRepository;
    @Autowired private ProductSkuJpaRepository productSkuJpaRepository;
    @Autowired private WarehouseJpaRepository warehouseJpaRepository;
    @Autowired private WarehouseSectionJpaRepository warehouseSectionJpaRepository;
    @Autowired private EntityManager entityManager;

    private Long skuId;
    private Long sectionR1;
    private Long sectionD1;
    private Long lotA;
    private Long invA;

    @BeforeEach
    void setUp() {
        skuId = productSkuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode("SKU-A").name("라켓 A").unit("EA")
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build()).getSkuId();
        Long warehouseId = warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode("WH-1").name("서울").address("주소").totalCapacity(BigDecimal.valueOf(1000))
                .status(WarehouseStatus.ACTIVE).build()).getWarehouseId();
        sectionR1 = section(warehouseId, "R1", "RACK", 200, 100);
        sectionD1 = section(warehouseId, "D1", "DEFECT", 50, 0);

        lotA = lotRepository.save(Lot.register(skuId, 3L, "LOT-A", null, LocalDate.of(2027, 1, 1),
                BigDecimal.valueOf(60000))).getLotId();
        InventoryLot il = InventoryLot.open(sectionR1, lotA, QualityStatus.AVAILABLE);
        il.increase(100);
        il.allocate(20);
        invA = inventoryLotRepository.save(il).getInventoryLotId();
        flushAndClear();
    }

    @Nested
    @DisplayName("재고 조정")
    class Adjust {

        @Test
        @DisplayName("감소 조정: 보유 수량·구역 사용 용량이 줄고 ADJUSTMENT 이력이 남는다")
        void decrease() {
            InventoryAdjustmentResult result = adjustmentUseCase.adjust(
                    new InventoryAdjustCommand(invA, 100L, 97L, " 실사 결과 3개 부족 ", USER));

            assertThat(result.transaction().getQuantityDelta()).isEqualTo(-3L);
            assertThat(result.transaction().getReferenceId()).isNull();
            assertThat(result.transaction().getReason()).isEqualTo("실사 결과 3개 부족");
            assertThat(result.inventoryLot().availableQuantity()).isEqualTo(77L);
            flushAndClear();
            assertThat(onHand(invA)).isEqualTo(97L);
            assertThat(currentCapacity(sectionR1)).isEqualByComparingTo("97");
        }

        @Test
        @DisplayName("증가 조정: 구역 사용 용량이 늘어난다")
        void increase() {
            adjustmentUseCase.adjust(new InventoryAdjustCommand(invA, 100L, 150L, "실사 초과분", USER));
            flushAndClear();
            assertThat(onHand(invA)).isEqualTo(150L);
            assertThat(currentCapacity(sectionR1)).isEqualByComparingTo("150");
        }

        @Test
        @DisplayName("화면에서 본 수량이 현재 값과 다르면 STALE_QUANTITY")
        void stale() {
            assertError(() -> adjustmentUseCase.adjust(new InventoryAdjustCommand(invA, 99L, 90L, "사유", USER)),
                    "STALE_QUANTITY");
        }

        @Test
        @DisplayName("할당 수량보다 작게 줄이면 BELOW_ALLOCATED_QUANTITY")
        void belowAllocated() {
            assertError(() -> adjustmentUseCase.adjust(new InventoryAdjustCommand(invA, 100L, 19L, "사유", USER)),
                    "BELOW_ALLOCATED_QUANTITY");
        }

        @Test
        @DisplayName("구역 수용량을 넘게 늘리면 SECTION_CAPACITY_EXCEEDED")
        void capacityExceeded() {
            assertError(() -> adjustmentUseCase.adjust(new InventoryAdjustCommand(invA, 100L, 201L, "사유", USER)),
                    "SECTION_CAPACITY_EXCEEDED");
        }

        @Test
        @DisplayName("변경 없음·사유 없음·없는 재고는 거절한다")
        void invalid() {
            assertError(() -> adjustmentUseCase.adjust(new InventoryAdjustCommand(invA, 100L, 100L, "사유", USER)),
                    "VALIDATION_ERROR");
            assertError(() -> adjustmentUseCase.adjust(new InventoryAdjustCommand(invA, 100L, 90L, " ", USER)),
                    "VALIDATION_ERROR");
            assertError(() -> adjustmentUseCase.adjust(new InventoryAdjustCommand(-1L, 100L, 90L, "사유", USER)),
                    "NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("입고 반영")
    class Receive {

        @Test
        @DisplayName("기존 행에는 더하고, 없는 구역·로트는 새 행을 만들며 행마다 INBOUND 이력을 남긴다")
        void receive() {
            List<InventoryLot> result = stockUseCase.receive(List.of(
                    new StockReceiveCommand(sectionR1, lotA, QualityStatus.AVAILABLE, 58, 7L, USER),
                    new StockReceiveCommand(sectionD1, lotA, QualityStatus.DEFECTIVE, 2, 7L, USER)));

            assertThat(result).hasSize(2);
            flushAndClear();
            assertThat(onHand(invA)).isEqualTo(158L);
            assertThat(currentCapacity(sectionR1)).isEqualByComparingTo("158");
            assertThat(currentCapacity(sectionD1)).isEqualByComparingTo("2");

            List<InventoryTransactionView> history = queryUseCase.getTransactions(new InventoryTransactionSearchCondition(
                    null, null, null, null, null, TransactionType.INBOUND, null, null, null, null));
            assertThat(history).hasSize(2)
                    .allSatisfy(t -> assertThat(t.referenceId()).isEqualTo(7L));
            assertThat(history).extracting(InventoryTransactionView::afterQuantity)
                    .containsExactlyInAnyOrder(158L, 2L);
        }

        @Test
        @DisplayName("가용 상태가 아닌 로트, 같은 구역·로트의 품질 상태 불일치, 수용량 초과는 거절한다")
        void reject() {
            Lot quarantined = Lot.register(skuId, 3L, "LOT-Q", null, null, BigDecimal.TEN);
            quarantined.quarantine();
            Long lotQ = lotRepository.save(quarantined).getLotId();

            assertError(() -> stockUseCase.receive(List.of(
                    new StockReceiveCommand(sectionR1, lotQ, QualityStatus.AVAILABLE, 1, 7L, USER))),
                    "LOT_NOT_AVAILABLE");
            assertError(() -> stockUseCase.receive(List.of(
                    new StockReceiveCommand(sectionR1, lotA, QualityStatus.DEFECTIVE, 1, 7L, USER))),
                    "CONFLICT");
            assertError(() -> stockUseCase.receive(List.of(
                    new StockReceiveCommand(sectionD1, lotA, QualityStatus.DEFECTIVE, 51, 7L, USER))),
                    "SECTION_CAPACITY_EXCEEDED");
        }
    }

    @Nested
    @DisplayName("할당·해제·출고")
    class AllocateAndShip {

        @Test
        @DisplayName("할당은 가용 수량 안에서만 되고, 부족하면 INSUFFICIENT_STOCK으로 아무것도 바뀌지 않는다")
        void allocate() {
            stockUseCase.allocate(List.of(new StockQuantityCommand(invA, 30)));
            flushAndClear();
            assertThat(allocated(invA)).isEqualTo(50L);

            assertError(() -> stockUseCase.allocate(List.of(new StockQuantityCommand(invA, 51))),
                    "INSUFFICIENT_STOCK");
            flushAndClear();
            assertThat(allocated(invA)).isEqualTo(50L);
        }

        @Test
        @DisplayName("해제는 할당 수량 안에서만 된다")
        void release() {
            stockUseCase.release(List.of(new StockQuantityCommand(invA, 20)));
            flushAndClear();
            assertThat(allocated(invA)).isZero();
            assertError(() -> stockUseCase.release(List.of(new StockQuantityCommand(invA, 1))), "CONFLICT");
        }

        @Test
        @DisplayName("피킹 완료: 할당 전체를 풀고 피킹 수량만 차감하며, 구역 용량·OUTBOUND 이력이 반영된다")
        void shipWithShortage() {
            stockUseCase.ship(List.of(new StockShipCommand(invA, 20, 18, 3L, USER)));
            flushAndClear();

            assertThat(onHand(invA)).isEqualTo(82L);
            assertThat(allocated(invA)).isZero();
            assertThat(currentCapacity(sectionR1)).isEqualByComparingTo("82");
            assertThat(queryUseCase.getTransactionsOf(invA, emptyCondition()))
                    .singleElement()
                    .satisfies(t -> {
                        assertThat(t.transactionType()).isEqualTo(TransactionType.OUTBOUND);
                        assertThat(t.quantityDelta()).isEqualTo(-18L);
                        assertThat(t.referenceId()).isEqualTo(3L);
                    });
        }

        @Test
        @DisplayName("피킹 0이면 할당만 풀리고 이력은 남지 않으며, 피킹 수량이 할당보다 크면 거절한다")
        void shipNothingPickedAndInvalid() {
            stockUseCase.ship(List.of(new StockShipCommand(invA, 20, 0, 3L, USER)));
            flushAndClear();
            assertThat(onHand(invA)).isEqualTo(100L);
            assertThat(allocated(invA)).isZero();
            assertThat(queryUseCase.getTransactionsOf(invA, emptyCondition())).isEmpty();

            assertError(() -> stockUseCase.ship(List.of(new StockShipCommand(invA, 5, 6, 3L, USER))),
                    "VALIDATION_ERROR");
        }

        @Test
        @DisplayName("없는 재고 행이 섞이면 NOT_FOUND")
        void notFound() {
            assertError(() -> stockUseCase.allocate(List.of(
                    new StockQuantityCommand(invA, 1), new StockQuantityCommand(-1L, 1))), "NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("로트 find-or-create")
    class FindOrRegisterLot {

        @Test
        @DisplayName("같은 SKU·공급처·로트 번호면 기존 로트를 재사용하고, 없으면 새로 만든다")
        void reuseOrCreate() {
            Lot same = lotUseCase.findOrRegister(new LotRegisterCommand(
                    skuId, 3L, "LOT-A", null, LocalDate.of(2027, 1, 1), new BigDecimal("60000.00")));
            assertThat(same.getLotId()).isEqualTo(lotA);

            Lot created = lotUseCase.findOrRegister(new LotRegisterCommand(
                    skuId, 3L, "LOT-NEW", null, null, BigDecimal.valueOf(55000)));
            assertThat(created.getLotId()).isNotEqualTo(lotA);
            assertThat(created.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("기존 로트와 원가·일자가 다르거나, 유통기한이 제조일보다 빠르면 거절한다")
        void mismatch() {
            assertError(() -> lotUseCase.findOrRegister(new LotRegisterCommand(
                    skuId, 3L, "LOT-A", null, LocalDate.of(2027, 1, 1), BigDecimal.valueOf(59000))),
                    "LOT_UNIT_COST_MISMATCH");
            assertError(() -> lotUseCase.findOrRegister(new LotRegisterCommand(
                    skuId, 3L, "LOT-A", null, LocalDate.of(2027, 6, 1), BigDecimal.valueOf(60000))),
                    "LOT_DATE_MISMATCH");
            assertError(() -> lotUseCase.findOrRegister(new LotRegisterCommand(
                    skuId, 3L, "LOT-X", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1), BigDecimal.TEN)),
                    "VALIDATION_ERROR");
        }
    }

    @Test
    @DisplayName("조회: 없는 재고·로트는 NOT_FOUND, 이력 기간이 뒤집히면 VALIDATION_ERROR")
    void queryErrors() {
        assertError(() -> queryUseCase.getInventory(-1L), "NOT_FOUND");
        assertError(() -> queryUseCase.getTransactionsOf(-1L, emptyCondition()), "NOT_FOUND");
        assertError(() -> lotUseCase.getLot(-1L), "NOT_FOUND");
        assertError(() -> queryUseCase.getTransactions(new InventoryTransactionSearchCondition(
                null, null, null, null, null, null, null, null,
                java.time.LocalDateTime.of(2026, 9, 2, 0, 0), java.time.LocalDateTime.of(2026, 9, 1, 0, 0))),
                "VALIDATION_ERROR");
        assertThat(queryUseCase.getInventory(invA).availableQuantity()).isEqualTo(80L);
    }

    // ---------- helpers ----------

    private Long section(Long warehouseId, String code, String type, int capacity, int used) {
        return warehouseSectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode(code).name(code).sectionType(type)
                .capacity(BigDecimal.valueOf(capacity)).currentCapacity(BigDecimal.valueOf(used))
                .status(WarehouseStatus.ACTIVE).build()).getSectionId();
    }

    private long onHand(Long id) {
        return inventoryLotRepository.findById(id).orElseThrow().getOnHandQuantity();
    }

    private long allocated(Long id) {
        return inventoryLotRepository.findById(id).orElseThrow().getAllocatedQuantity();
    }

    private BigDecimal currentCapacity(Long sectionId) {
        return warehouseSectionRepository.findById(sectionId).orElseThrow().getCurrentCapacity();
    }

    private static InventoryTransactionSearchCondition emptyCondition() {
        return new InventoryTransactionSearchCondition(null, null, null, null, null, null, null, null, null, null);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCodeName()).isEqualTo(errorCode));
    }
}

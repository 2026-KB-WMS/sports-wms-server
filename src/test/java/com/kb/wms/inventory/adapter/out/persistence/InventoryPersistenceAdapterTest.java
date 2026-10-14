package com.kb.wms.inventory.adapter.out.persistence;

import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.common.security.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySectionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventoryTransactionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LowStockSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.application.port.in.result.InventorySectionSummary;
import com.kb.wms.inventory.application.port.in.result.InventorySkuSummary;
import com.kb.wms.inventory.application.port.in.result.LowStockItem;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.InventoryQueryRepository;
import com.kb.wms.inventory.application.port.out.InventoryTransactionRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.entity.InventoryTransaction;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;
import com.kb.wms.product.adapter.out.persistence.entity.CategoryJpaEntity;
import com.kb.wms.product.adapter.out.persistence.entity.ProductJpaEntity;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.CategoryJpaRepository;
import com.kb.wms.product.adapter.out.persistence.repository.ProductJpaRepository;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseSectionJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import jakarta.persistence.EntityManager;

/**
 * 재고 영속성 어댑터(저장·잠금 조회·조회 전용 쿼리) 검증.
 *
 * <p>데이터: SKU-A(안전재고 50), SKU-B(안전재고 10, 재고 없음), SKU-C(안전재고 0)
 * <ul>
 *   <li>창고1: 일반 구역 R1, 불량 구역 D1 / 창고2: 일반 구역 R2</li>
 *   <li>SKU-A 로트: LOT-A(유통기한 2027-01-01), LOT-B(유통기한 없음), LOT-Q(격리)</li>
 *   <li>재고: R1·LOT-A 100(할당 20), D1·LOT-A 5(불량), R2·LOT-B 30, R1·LOT-Q 10</li>
 * </ul>
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class InventoryPersistenceAdapterTest {

    @Autowired private UserRepository userRepository;
    @Autowired private InventoryQueryRepository inventoryQueryRepository;
    @Autowired private InventoryLotRepository inventoryLotRepository;
    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired private LotRepository lotRepository;
    @Autowired private ProductSkuJpaRepository productSkuJpaRepository;
    @Autowired private ProductJpaRepository productJpaRepository;
    @Autowired private CategoryJpaRepository categoryJpaRepository;
    @Autowired private WarehouseJpaRepository warehouseJpaRepository;
    @Autowired private WarehouseSectionJpaRepository warehouseSectionJpaRepository;
    @Autowired private EntityManager entityManager;

    private Long skuA;
    private Long skuB;
    private Long warehouse1;
    private Long warehouse2;
    private Long sectionR1;
    private Long sectionD1;
    private Long lotA;
    private Long lotQ;
    private Long invR1LotA;
    private Long invD1LotA;
    private Long invR2LotB;
    private Long invR1LotQ;

    @BeforeEach
    void setUp() {
        skuA = sku("SKU-A", "배드민턴 라켓 A", 50L);
        skuB = sku("SKU-B", "셔틀콕 B", 10L);
        sku("SKU-C", "그립 C", 0L);

        warehouse1 = warehouse("WH-1", "서울 물류센터");
        warehouse2 = warehouse("WH-2", "부산 물류센터");
        sectionR1 = section(warehouse1, "R1", "RACK");
        sectionD1 = section(warehouse1, "D1", "DEFECT");
        Long sectionR2 = section(warehouse2, "R2", "RACK");

        lotA = lot(skuA, "LOT-A", LocalDate.of(2027, 1, 1));
        Long lotB = lot(skuA, "LOT-B", null);
        Lot quarantined = Lot.register(skuA, 3L, "LOT-Q", null, null, BigDecimal.valueOf(60000));
        quarantined.quarantine();
        lotQ = lotRepository.save(quarantined).getLotId();

        invR1LotA = inventory(sectionR1, lotA, QualityStatus.AVAILABLE, 100L, 20L);
        invD1LotA = inventory(sectionD1, lotA, QualityStatus.DEFECTIVE, 5L, 0L);
        invR2LotB = inventory(sectionR2, lotB, QualityStatus.AVAILABLE, 30L, 0L);
        invR1LotQ = inventory(sectionR1, lotQ, QualityStatus.AVAILABLE, 10L, 0L);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("SKU 집계: 가용 수량은 품질·로트 상태가 모두 AVAILABLE인 행만 합산하고 불량은 따로 집계한다")
    void skuSummary_allWarehouses() {
        List<InventorySkuSummary> result =
                inventoryQueryRepository.findSkuSummaries(InventorySearchCondition.unscoped(null, null, null));

        assertThat(result).hasSize(1);
        InventorySkuSummary a = result.get(0);
        assertThat(a.skuCode()).isEqualTo("SKU-A");
        assertThat(a.totalQuantity()).isEqualTo(145L);      // 100 + 5 + 30 + 10
        assertThat(a.allocatedQuantity()).isEqualTo(20L);
        assertThat(a.defectiveQuantity()).isEqualTo(5L);
        assertThat(a.availableQuantity()).isEqualTo(110L);  // (100-20) + 30, 격리 로트·불량 제외
    }

    @Test
    @DisplayName("SKU 집계: 창고 필터와 키워드(SKU명 부분 일치)가 적용된다")
    void skuSummary_filter() {
        List<InventorySkuSummary> result =
                inventoryQueryRepository.findSkuSummaries(InventorySearchCondition.unscoped(null, warehouse1, "라켓"));

        assertThat(result).singleElement().satisfies(a -> {
            assertThat(a.totalQuantity()).isEqualTo(115L);
            assertThat(a.availableQuantity()).isEqualTo(80L);
        });
        assertThat(inventoryQueryRepository.findSkuSummaries(InventorySearchCondition.unscoped(null, null, "셔틀콕")))
                .isEmpty();
    }

    @Test
    @DisplayName("로트 단위 조회: 유통기한 오름차순(없으면 뒤로)이고 불량·격리 행의 가용 수량은 0이다")
    void lotViews() {
        List<InventoryLotView> result = inventoryQueryRepository.findLotViews(
                InventoryLotSearchCondition.unscoped(null, warehouse1, null, null, null, null, false));

        assertThat(result).extracting(InventoryLotView::inventoryLotId)
                .containsExactly(invR1LotA, invD1LotA, invR1LotQ);
        assertThat(result).extracting(InventoryLotView::availableQuantity)
                .containsExactly(80L, 0L, 0L);
        assertThat(result.get(0).sectionCode()).isEqualTo("R1");
    }

    @Test
    @DisplayName("창고 범위: 담당 창고 목록으로 집계·로트 단위·이력을 좁히고, 빈 목록이면 아무것도 보이지 않는다")
    void warehouseScope_restrictsQueries() {
        inventoryTransactionRepository.save(InventoryTransaction.record(
                invR1LotA, TransactionType.INBOUND, 0, 100, ReferenceType.INBOUND, 6L, null, 5L));
        inventoryTransactionRepository.save(InventoryTransaction.record(
                invR2LotB, TransactionType.INBOUND, 0, 30, ReferenceType.INBOUND, 7L, null, 5L));
        entityManager.flush();

        // 창고2만 담당: SKU-A 집계는 창고2 재고(30)뿐이다
        assertThat(inventoryQueryRepository.findSkuSummaries(
                new InventorySearchCondition(null, null, null, List.of(warehouse2))))
                .singleElement().satisfies(a -> assertThat(a.totalQuantity()).isEqualTo(30L));
        // 두 창고 모두 담당이면 합산한다
        assertThat(inventoryQueryRepository.findSkuSummaries(
                new InventorySearchCondition(null, null, null, List.of(warehouse1, warehouse2))))
                .singleElement().satisfies(a -> assertThat(a.totalQuantity()).isEqualTo(145L));
        // 담당 창고가 없으면 비어 있다
        assertThat(inventoryQueryRepository.findSkuSummaries(
                new InventorySearchCondition(null, null, null, List.of()))).isEmpty();

        assertThat(inventoryQueryRepository.findLotViews(new InventoryLotSearchCondition(
                null, null, null, null, null, null, false, List.of(warehouse2))))
                .extracting(InventoryLotView::inventoryLotId).containsExactly(invR2LotB);

        assertThat(inventoryQueryRepository.findTransactions(new InventoryTransactionSearchCondition(
                null, null, null, null, null, null, null, null, null, null, List.of(warehouse1))))
                .singleElement().satisfies(t -> assertThat(t.referenceId()).isEqualTo(6L));
        assertThat(inventoryQueryRepository.findTransactions(new InventoryTransactionSearchCondition(
                null, null, null, null, null, null, null, null, null, null, List.of()))).isEmpty();

        // 안전 재고 미만: 담당 창고2만 보면 SKU-A 가용 30 < 50 (부족 20), 창고1 재고는 합산되지 않는다
        assertThat(inventoryQueryRepository.findLowStock(new LowStockSearchCondition(null, null, List.of(warehouse2))))
                .extracting(LowStockItem::skuCode, LowStockItem::availableQuantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("SKU-A", 30L),
                        org.assertj.core.groups.Tuple.tuple("SKU-B", 0L));
    }

    @Test
    @DisplayName("재고 상세: 창고명·구역·로트 정보를 함께 반환한다")
    void detail() {
        assertThat(inventoryQueryRepository.findDetail(invR1LotA)).hasValueSatisfying(d -> {
            assertThat(d.warehouseName()).isEqualTo("서울 물류센터");
            assertThat(d.lotNumber()).isEqualTo("LOT-A");
            assertThat(d.availableQuantity()).isEqualTo(80L);
            assertThat(d.createdAt()).isNotNull();
        });
        assertThat(inventoryQueryRepository.findDetail(-1L)).isEmpty();
    }

    @Test
    @DisplayName("안전 재고 미만: 창고 범위 가용 재고로 비교하고, 재고 없는 SKU는 가용 0으로 포함, 안전재고 0은 제외")
    void lowStock() {
        // 창고1: SKU-A 가용 80 > 50 이라 제외, SKU-B 가용 0 < 10
        assertThat(inventoryQueryRepository.findLowStock(LowStockSearchCondition.unscoped(warehouse1, null)))
                .extracting(LowStockItem::skuCode, LowStockItem::availableQuantity, LowStockItem::shortageQuantity)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("SKU-B", 0L, 10L));

        // 창고2: SKU-A 가용 30 <= 50 (부족 20), SKU-B 부족 10 → 부족 수량 내림차순
        assertThat(inventoryQueryRepository.findLowStock(LowStockSearchCondition.unscoped(warehouse2, null)))
                .extracting(LowStockItem::skuCode, LowStockItem::shortageQuantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("SKU-A", 20L),
                        org.assertj.core.groups.Tuple.tuple("SKU-B", 10L));
    }

    @Test
    @DisplayName("할당 후보: 불량·격리·가용 0 행을 제외하고 FEFO 순서로 반환한다")
    void allocatableStocks() {
        assertThat(inventoryQueryRepository.findAllocatableStocks(warehouse1, skuA))
                .extracting(InventoryLotView::inventoryLotId)
                .containsExactly(invR1LotA);
        assertThat(inventoryQueryRepository.findAllocatableStocks(warehouse1, skuB)).isEmpty();
    }

    @Test
    @DisplayName("잠금 조회: 여러 행을 inventory_lot_id 오름차순으로 반환하고, 빈 목록이면 쿼리 없이 빈 결과")
    void findAllByIdsForUpdate() {
        List<InventoryLot> locked =
                inventoryLotRepository.findAllByIdsForUpdate(List.of(invR1LotQ, invR2LotB, invR1LotA));

        assertThat(locked).extracting(InventoryLot::getInventoryLotId)
                .isSorted()
                .containsExactlyInAnyOrder(invR1LotA, invR2LotB, invR1LotQ);
        assertThat(inventoryLotRepository.findAllByIdsForUpdate(List.of())).isEmpty();
        assertThat(inventoryLotRepository.findBySectionIdAndLotIdForUpdate(sectionR1, lotA))
                .hasValueSatisfying(il -> assertThat(il.getInventoryLotId()).isEqualTo(invR1LotA));
    }

    @Test
    @DisplayName("로트 단위 잠금 조회: 그 로트의 모든 재고 행(품질 불량 포함)만 inventory_lot_id 오름차순으로 반환하고 로트 행도 잠금 조회된다")
    void findAllByLotIdForUpdate() {
        assertThat(inventoryLotRepository.findAllByLotIdForUpdate(lotA))
                .extracting(InventoryLot::getInventoryLotId).containsExactly(invR1LotA, invD1LotA);
        assertThat(inventoryLotRepository.findAllByLotIdForUpdate(999999L)).isEmpty();
        assertThat(lotRepository.findByIdForUpdate(lotA)).isPresent();
        assertThat(lotRepository.findByIdForUpdate(999999L)).isEmpty();
    }

    @Test
    @DisplayName("잠금 조회 후 수량을 바꿔 저장하면 반영된다")
    void lockAndSave() {
        InventoryLot il = inventoryLotRepository.findByIdForUpdate(invR1LotA).orElseThrow();
        il.allocate(30);
        inventoryLotRepository.save(il);
        entityManager.flush();
        entityManager.clear();

        assertThat(inventoryLotRepository.findById(invR1LotA))
                .hasValueSatisfying(saved -> assertThat(saved.getAllocatedQuantity()).isEqualTo(50L));
    }

    @Test
    @DisplayName("이력: 저장한 이력을 재고 행·유형 조건으로 최신순 조회한다")
    void transactions() {
        inventoryTransactionRepository.save(InventoryTransaction.record(
                invR1LotA, TransactionType.INBOUND, 0, 100, ReferenceType.INBOUND, 6L, null, 5L));
        inventoryTransactionRepository.save(InventoryTransaction.record(
                invR1LotA, TransactionType.ADJUSTMENT, 100, 97, ReferenceType.ADJUSTMENT, null, "실사 차이", 5L));
        entityManager.flush();

        assertThat(inventoryQueryRepository.findTransactions(InventoryTransactionSearchCondition.unscoped(
                invR1LotA, null, null, null, null, null, null, null, null, null)))
                .hasSize(2)
                .first()
                .satisfies(t -> {
                    assertThat(t.transactionType()).isEqualTo(TransactionType.ADJUSTMENT);
                    assertThat(t.quantityDelta()).isEqualTo(-3L);
                    assertThat(t.sectionCode()).isEqualTo("R1");
                    assertThat(t.lotNumber()).isEqualTo("LOT-A");
                });

        assertThat(inventoryQueryRepository.findTransactions(InventoryTransactionSearchCondition.unscoped(
                null, warehouse1, null, null, null, TransactionType.INBOUND, null, null, null, null)))
                .singleElement()
                .satisfies(t -> assertThat(t.referenceId()).isEqualTo(6L));
    }

    @Test
    @DisplayName("로트: 유통기한 조건·로트 번호 검색, 단건 조회, find-or-create용 유니크 키 조회")
    void lots() {
        assertThat(inventoryQueryRepository.findLots(
                LotSearchCondition.unscoped(skuA, null, LocalDate.of(2027, 12, 31), null)))
                .extracting(l -> l.lotNumber())
                .containsExactly("LOT-A");
        assertThat(inventoryQueryRepository.findLots(LotSearchCondition.unscoped(null, null, null, "lot-q")))
                .singleElement()
                .satisfies(l -> assertThat(l.skuCode()).isEqualTo("SKU-A"));
        assertThat(inventoryQueryRepository.findLot(lotQ)).isPresent();
        assertThat(lotRepository.findBySkuIdAndSupplierIdAndLotNumber(skuA, 3L, "LOT-A")).isPresent();
    }

    @Test
    @DisplayName("같은 구역·로트로 재고 행을 두 번 저장하면 UNIQUE(section_id, lot_id) 제약을 위반한다")
    void inventoryLot_duplicateSectionAndLot_violatesUniqueConstraint() {
        // IDENTITY 채번 전략이라 save() 시점에 즉시 INSERT가 나가 제약 위반도 그때 발생한다(flush 대기 없음).
        assertThatThrownBy(() -> {
            InventoryLot duplicate = InventoryLot.open(sectionR1, lotA, QualityStatus.AVAILABLE);
            duplicate.increase(1);
            inventoryLotRepository.save(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 SKU·공급처·로트 번호로 로트를 두 번 저장하면 UNIQUE(sku_id, supplier_id, lot_number) 제약을 위반한다")
    void lot_duplicateSkuSupplierLotNumber_violatesUniqueConstraint() {
        assertThatThrownBy(() -> lotRepository.save(
                Lot.register(skuA, 3L, "LOT-A", null, LocalDate.of(2028, 1, 1), BigDecimal.valueOf(70000))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- fixtures ----------

    @Test
    @DisplayName("재고 존재 확인: 보유·할당 수량이 있는 구역·창고만 true, 재고 행이 없거나 0뿐이면 false")
    void existsStock() {
        Long emptySection = section(warehouse1, "E1", "RACK");
        Long emptyWarehouse = warehouse("WH-3", "대구 물류센터");
        inventoryLotRepository.save(InventoryLot.open(emptySection, lotA, QualityStatus.AVAILABLE));
        entityManager.flush();

        assertThat(inventoryQueryRepository.existsStockInSection(sectionR1)).isTrue();
        assertThat(inventoryQueryRepository.existsStockInSection(emptySection)).isFalse();
        assertThat(inventoryQueryRepository.existsStockInWarehouse(warehouse2)).isTrue();
        assertThat(inventoryQueryRepository.existsStockInWarehouse(emptyWarehouse)).isFalse();
    }

    // ---------- fixtures ----------

    private Long sku(String code, String name, long safetyStock) {
        return productSkuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode(code).name(name).unit("EA").currentPurchasePrice(java.math.BigDecimal.ZERO).currentSupplyPrice(java.math.BigDecimal.ZERO)
                .safetyStockQuantity(safetyStock).status(ProductStatus.ACTIVE)
                .build()).getSkuId();
    }

    private Long warehouse(String code, String name) {
        return warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode(code).name(name).address("주소").totalCapacity(BigDecimal.valueOf(1000))
                .status(WarehouseStatus.ACTIVE)
                .build()).getWarehouseId();
    }

    private Long section(Long warehouseId, String code, String type) {
        return warehouseSectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode(code).name(code + " 구역").sectionType(type)
                .capacity(BigDecimal.valueOf(500)).currentCapacity(BigDecimal.ZERO)
                .status(WarehouseStatus.ACTIVE)
                .build()).getSectionId();
    }

    private Long lot(Long skuId, String lotNumber, LocalDate expiryDate) {
        return lotRepository.save(Lot.register(skuId, 3L, lotNumber, null, expiryDate, BigDecimal.valueOf(60000)))
                .getLotId();
    }

    private Long inventory(Long sectionId, Long lotId, QualityStatus quality, long onHand, long allocated) {
        InventoryLot il = InventoryLot.open(sectionId, lotId, quality);
        il.increase(onHand);
        if (allocated > 0) {
            il.allocate(allocated);
        }
        return inventoryLotRepository.save(il).getInventoryLotId();
    }

    @Test
    @DisplayName("이력: 처리자 이름을 사용자 테이블 조인으로 담고, 사용자가 없으면 이름만 null이다")
    void transactions_createdByName() {
        User writer = userRepository.save(User.signUp("tx_writer", "hashed", "김처리", "tx_writer@example.com",
                "010-1234-5678", UserRole.WAREHOUSE_MANAGER));
        inventoryTransactionRepository.save(InventoryTransaction.record(
                invR1LotA, TransactionType.INBOUND, 0, 100, ReferenceType.INBOUND, 6L, null, writer.getUserId()));
        inventoryTransactionRepository.save(InventoryTransaction.record(
                invR1LotA, TransactionType.ADJUSTMENT, 100, 97, ReferenceType.ADJUSTMENT, null, "실사 차이",
                writer.getUserId() + 1_000_000L));  // 존재하지 않는 사용자 ID (방금 만든 사용자와 겹치지 않는다)
        entityManager.flush();

        var result = inventoryQueryRepository.findTransactions(InventoryTransactionSearchCondition.unscoped(
                invR1LotA, null, null, null, null, null, null, null, null, null));

        assertThat(result).hasSize(2);
        assertThat(result.stream().filter(t -> t.transactionType() == TransactionType.INBOUND).findFirst()
                .orElseThrow().createdByName()).isEqualTo("김처리");
        assertThat(result.stream().filter(t -> t.transactionType() == TransactionType.ADJUSTMENT).findFirst()
                .orElseThrow().createdByName()).isNull();
    }

    // H2(PostgreSQL 모드)가 category 엔티티의 TINYINT 컬럼을 만들지 못해 이 테이블만 직접 만든다.
    @Sql(statements = "create table if not exists category (category_id bigint generated by default as identity, "
            + "parent_category_id bigint, category_code varchar(50) not null, name varchar(100) not null, "
            + "depth smallint not null, sort_order integer not null, status varchar(20) not null, "
            + "created_at timestamp(6) not null, updated_at timestamp(6) not null, primary key (category_id))",
            config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
    @Test
    @DisplayName("구역 × SKU 집계: 창고·구역·SKU 코드 순으로 구역별 수량을 합산하고, 카테고리·창고 범위·보유 0 행 제외가 적용된다")
    void sectionSummaries() {
        Long racket = category("CAT-RK", "라켓");
        Long shoes = category("CAT-SH", "신발");
        Long racketSku = skuOf(product(racket, "P-RK", "프로 라켓"), "SKU-X1", "프로 라켓 3U");
        Long shoesSku = skuOf(product(shoes, "P-SH", "러닝화"), "SKU-X2", "러닝화 260");
        Long lotX1 = lot(racketSku, "LOT-X1", null);
        Long lotX2 = lot(shoesSku, "LOT-X2", null);
        inventory(sectionR1, lotX1, QualityStatus.AVAILABLE, 40L, 10L);
        inventory(sectionD1, lotX1, QualityStatus.DEFECTIVE, 3L, 0L);
        Long sectionR2 = warehouseSectionJpaRepository.findAll().stream()
                .filter(s -> s.getWarehouseId().equals(warehouse2)).findFirst().orElseThrow().getSectionId();
        inventory(sectionR2, lotX2, QualityStatus.AVAILABLE, 20L, 0L);
        inventoryLotRepository.save(InventoryLot.open(
                sectionR1, lot(shoesSku, "LOT-X0", null), QualityStatus.AVAILABLE));  // 보유 0은 제외
        entityManager.flush();
        entityManager.clear();

        List<InventorySectionSummary> all = inventoryQueryRepository.findSectionSummaries(
                InventorySectionSearchCondition.unscoped(null, null, null, null, null)).stream()
                .filter(r -> r.skuId().equals(racketSku) || r.skuId().equals(shoesSku)).toList();

        // 창고1 D1 → 창고1 R1 → 창고2 R2 순서
        assertThat(all).extracting(InventorySectionSummary::sectionCode, InventorySectionSummary::skuCode)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("D1", "SKU-X1"),
                        org.assertj.core.groups.Tuple.tuple("R1", "SKU-X1"),
                        org.assertj.core.groups.Tuple.tuple("R2", "SKU-X2"));
        InventorySectionSummary r1 = all.get(1);
        assertThat(r1.warehouseName()).isEqualTo("서울 물류센터");
        assertThat(r1.categoryName()).isEqualTo("라켓");
        assertThat(r1.totalQuantity()).isEqualTo(40L);
        assertThat(r1.allocatedQuantity()).isEqualTo(10L);
        assertThat(r1.availableQuantity()).isEqualTo(30L);
        assertThat(r1.defectiveQuantity()).isEqualTo(0L);
        assertThat(all.get(0).defectiveQuantity()).isEqualTo(3L);
        assertThat(all.get(0).availableQuantity()).isEqualTo(0L);

        // 카테고리 필터
        assertThat(inventoryQueryRepository.findSectionSummaries(
                InventorySectionSearchCondition.unscoped(null, null, shoes, null, null)))
                .extracting(InventorySectionSummary::skuId).containsExactly(shoesSku);
        // 구역·창고·SKU 키워드 필터
        assertThat(inventoryQueryRepository.findSectionSummaries(
                InventorySectionSearchCondition.unscoped(null, sectionD1, null, racketSku, null)))
                .extracting(InventorySectionSummary::sectionCode).containsExactly("D1");
        assertThat(inventoryQueryRepository.findSectionSummaries(
                InventorySectionSearchCondition.unscoped(warehouse2, null, null, null, "러닝화")))
                .extracting(InventorySectionSummary::skuId).containsExactly(shoesSku);
        // 담당 창고 범위: 창고2만 담당이면 창고1 구역은 보이지 않고, 빈 목록이면 비어 있다
        assertThat(inventoryQueryRepository.findSectionSummaries(
                new InventorySectionSearchCondition(null, null, null, null, null, List.of(warehouse2))))
                .extracting(InventorySectionSummary::warehouseId).containsOnly(warehouse2);
        assertThat(inventoryQueryRepository.findSectionSummaries(
                new InventorySectionSearchCondition(null, null, null, null, null, List.of()))).isEmpty();
    }

    private Long category(String code, String name) {
        return categoryJpaRepository.save(CategoryJpaEntity.builder()
                .categoryCode(code).name(name).depth(1).sortOrder(1).status(ProductStatus.ACTIVE)
                .build()).getCategoryId();
    }

    private Long product(Long categoryId, String code, String name) {
        return productJpaRepository.save(ProductJpaEntity.builder()
                .brandId(1L).categoryId(categoryId).productCode(code).name(name).status(ProductStatus.ACTIVE)
                .build()).getProductId();
    }

    private Long skuOf(Long productId, String code, String name) {
        return productSkuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(productId).skuCode(code).name(name).unit("EA")
                .currentPurchasePrice(BigDecimal.ZERO).currentSupplyPrice(BigDecimal.ZERO)
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE)
                .build()).getSkuId();
    }
}

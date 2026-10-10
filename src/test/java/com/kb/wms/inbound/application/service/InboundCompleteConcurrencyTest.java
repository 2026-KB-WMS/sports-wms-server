package com.kb.wms.inbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.adapter.out.persistence.repository.InboundJpaRepository;
import com.kb.wms.inbound.adapter.out.persistence.repository.InboundLineJpaRepository;
import com.kb.wms.inbound.adapter.out.persistence.repository.PurchaseOrderJpaRepository;
import com.kb.wms.inbound.adapter.out.persistence.repository.PurchaseOrderLineJpaRepository;
import com.kb.wms.inbound.adapter.out.persistence.repository.SupplierJpaRepository;
import com.kb.wms.inbound.application.port.in.InboundCompleteUseCase;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.InboundLine;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryTransactionJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryTransactionJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.LotJpaRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseSectionJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

/**
 * 입고 완료의 동시성 검증. 입고 행·발주 행·구역 행·재고 행의 비관적 락이 갱신 유실과 중복 반영을 막는지 본다.
 *
 * <p>테스트 메서드는 트랜잭션을 걸지 않는다(각 스레드가 자신의 트랜잭션·커넥션을 얻어야 실제 동시성이 재현된다).
 * 대신 각 테스트가 만든 행을 {@code @AfterEach}에서 명시적으로 지운다.
 *
 * <p>각 입고는 같은 로트를 합격 8·불량 2(입고 10)로 받고, 합격은 같은 합격 구역, 불량은 같은 불량 구역에 적치한다.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class InboundCompleteConcurrencyTest {

    private static final AuthenticatedUser ACTOR =
            new AuthenticatedUser(5L, UserRole.HQ_ADMIN, List.of(), List.of());
    private static final long USER = 5L;
    private static final long RECEIVED = 10L;
    private static final long ACCEPTED = 8L;
    private static final long DEFECTIVE = 2L;
    private static final long EXPECTED = 20L;

    @Autowired private InboundCompleteUseCase completeUseCase;
    @Autowired private InboundRepository inboundRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private LotRepository lotRepository;

    @Autowired private InboundJpaRepository inboundJpaRepository;
    @Autowired private InboundLineJpaRepository inboundLineJpaRepository;
    @Autowired private PurchaseOrderJpaRepository purchaseOrderJpaRepository;
    @Autowired private PurchaseOrderLineJpaRepository purchaseOrderLineJpaRepository;
    @Autowired private SupplierJpaRepository supplierJpaRepository;
    @Autowired private InventoryLotJpaRepository inventoryLotJpaRepository;
    @Autowired private InventoryTransactionJpaRepository inventoryTransactionJpaRepository;
    @Autowired private LotJpaRepository lotJpaRepository;
    @Autowired private ProductSkuJpaRepository productSkuJpaRepository;
    @Autowired private WarehouseJpaRepository warehouseJpaRepository;
    @Autowired private WarehouseSectionJpaRepository warehouseSectionJpaRepository;

    private Long skuId;
    private Long warehouseId;
    private Long acceptedSectionId;
    private Long defectSectionId;
    private Long lotId;
    private Long supplierId;

    private final List<Long> purchaseOrderIds = new ArrayList<>();
    private final List<Long> purchaseOrderLineIds = new ArrayList<>();
    private final List<Long> inboundIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        skuId = productSkuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode("SKU-IBCONC").name("입고 동시성 테스트 상품").unit("EA").currentPurchasePrice(java.math.BigDecimal.ZERO).currentSupplyPrice(java.math.BigDecimal.ZERO)
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build()).getSkuId();
        warehouseId = warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode("WH-IBCONC").name("입고 동시성 테스트 창고").address("주소")
                .totalCapacity(BigDecimal.valueOf(100000)).status(WarehouseStatus.ACTIVE).build()).getWarehouseId();
        acceptedSectionId = warehouseSectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode("IBC-A").name("IBC-A").sectionType("RACK")
                .capacity(BigDecimal.valueOf(10000)).currentCapacity(BigDecimal.ZERO)
                .status(WarehouseStatus.ACTIVE).build()).getSectionId();
        defectSectionId = warehouseSectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode("IBC-D").name("IBC-D").sectionType("DEFECT")
                .capacity(BigDecimal.valueOf(10000)).currentCapacity(BigDecimal.ZERO)
                .status(WarehouseStatus.ACTIVE).build()).getSectionId();
        lotId = lotRepository.save(Lot.register(skuId, 1L, "LOT-IBCONC", null, null, BigDecimal.TEN)).getLotId();
        supplierId = supplierRepository.save(Supplier.register(
                "SUP-IBCONC", "입고 동시성 공급처", "담당자", "010-0000-0000", null, null)).getSupplierId();
    }

    @AfterEach
    void tearDown() {
        // 재고 행은 테스트 중에 생성되므로 구역 기준으로 찾아 이력부터 지운다.
        List<Long> sectionIds = List.of(acceptedSectionId, defectSectionId);
        List<InventoryLotJpaEntity> inventoryLots = inventoryLotJpaRepository.findAll().stream()
                .filter(lot -> sectionIds.contains(lot.getSectionId()))
                .toList();
        List<Long> inventoryLotIds = inventoryLots.stream().map(InventoryLotJpaEntity::getInventoryLotId).toList();
        List<InventoryTransactionJpaEntity> transactions = inventoryTransactionJpaRepository.findAll().stream()
                .filter(transaction -> inventoryLotIds.contains(transaction.getInventoryLotId()))
                .toList();
        inventoryTransactionJpaRepository.deleteAll(transactions);
        inventoryLotJpaRepository.deleteAll(inventoryLots);

        inboundIds.forEach(id -> {
            inboundLineJpaRepository.deleteAll(inboundLineJpaRepository.findAll().stream()
                    .filter(line -> id.equals(line.getInboundId())).toList());
            inboundJpaRepository.deleteById(id);
        });
        purchaseOrderLineIds.forEach(purchaseOrderLineJpaRepository::deleteById);
        purchaseOrderIds.forEach(purchaseOrderJpaRepository::deleteById);
        supplierJpaRepository.deleteById(supplierId);
        lotJpaRepository.deleteById(lotId);
        warehouseSectionJpaRepository.deleteById(acceptedSectionId);
        warehouseSectionJpaRepository.deleteById(defectSectionId);
        warehouseJpaRepository.deleteById(warehouseId);
        productSkuJpaRepository.deleteById(skuId);
    }

    @Test
    @DisplayName("같은 로트·구역에 서로 다른 입고 여러 건을 동시에 완료해도 재고와 구역 적재량이 합계와 정확히 일치한다")
    void concurrentComplete_sameLotAndSection_keepsQuantitiesConsistent() throws Exception {
        int inboundCount = 8;
        List<Long> targets = new ArrayList<>();
        for (int i = 0; i < inboundCount; i++) {
            targets.add(prepareInspectingInbound("A" + i));
        }

        List<Throwable> failures = runConcurrently(targets.stream()
                .<Callable<Void>>map(inboundId -> () -> {
                    completeUseCase.completeInbound(inboundId, ACTOR);
                    return null;
                })
                .toList());

        assertThat(failures).containsOnlyNulls();
        assertThat(quantityOf(acceptedSectionId, false)).isEqualTo(ACCEPTED * inboundCount);
        assertThat(quantityOf(defectSectionId, true)).isEqualTo(DEFECTIVE * inboundCount);
        assertThat(currentCapacityOf(acceptedSectionId)).isEqualByComparingTo(BigDecimal.valueOf(ACCEPTED * inboundCount));
        assertThat(currentCapacityOf(defectSectionId)).isEqualByComparingTo(BigDecimal.valueOf(DEFECTIVE * inboundCount));
        assertThat(inventoryLotJpaRepository.findAll().stream()
                .filter(lot -> lotId.equals(lot.getLotId())
                        && List.of(acceptedSectionId, defectSectionId).contains(lot.getSectionId()))
                .count()).as("(구역, 로트)당 재고 행은 하나만 만들어진다").isEqualTo(2);

        for (Long inboundId : targets) {
            assertThat(inboundRepository.findById(inboundId).orElseThrow().getStatus()).isEqualTo(InboundStatus.COMPLETED);
        }
        for (Long purchaseOrderLineId : purchaseOrderLineIds) {
            PurchaseOrderLine line = purchaseOrderRepository.findLineById(purchaseOrderLineId).orElseThrow();
            assertThat(line.getReceivedQuantity()).isEqualTo(RECEIVED);
            assertThat(line.getStatus()).isEqualTo(PurchaseOrderLineStatus.PARTIALLY_RECEIVED);
        }
    }

    @Test
    @DisplayName("같은 입고를 동시에 여러 번 완료 요청하면 한 번만 반영되고 나머지는 409로 거절된다")
    void concurrentComplete_sameInbound_appliesOnlyOnce() throws Exception {
        Long inboundId = prepareInspectingInbound("B");
        int threadCount = 6;

        List<Throwable> failures = runConcurrently(java.util.stream.IntStream.range(0, threadCount)
                .<Callable<Void>>mapToObj(i -> () -> {
                    completeUseCase.completeInbound(inboundId, ACTOR);
                    return null;
                })
                .toList());

        long succeeded = failures.stream().filter(java.util.Objects::isNull).count();
        List<Throwable> rejected = failures.stream().filter(java.util.Objects::nonNull).toList();
        assertThat(succeeded).isEqualTo(1);
        assertThat(rejected).hasSize(threadCount - 1)
                .allSatisfy(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).getErrorCodeName()).isEqualTo(ErrorCode.CONFLICT.name());
                });

        assertThat(quantityOf(acceptedSectionId, false)).isEqualTo(ACCEPTED);
        assertThat(quantityOf(defectSectionId, true)).isEqualTo(DEFECTIVE);
        assertThat(currentCapacityOf(acceptedSectionId)).isEqualByComparingTo(BigDecimal.valueOf(ACCEPTED));
        assertThat(currentCapacityOf(defectSectionId)).isEqualByComparingTo(BigDecimal.valueOf(DEFECTIVE));
        assertThat(purchaseOrderRepository.findLineById(purchaseOrderLineIds.get(0)).orElseThrow().getReceivedQuantity())
                .isEqualTo(RECEIVED);
        assertThat(inboundRepository.findById(inboundId).orElseThrow().getStatus()).isEqualTo(InboundStatus.COMPLETED);
    }

    /** 확정된 발주(항목 1줄, 발주 수량 20)와 검수 중 입고(입고 10 = 합격 8 + 불량 2)를 만든다. 입고 ID를 돌려준다. */
    private Long prepareInspectingInbound(String suffix) {
        PurchaseOrder purchaseOrder = PurchaseOrder.register(
                "PO-IBCONC-" + suffix, warehouseId, supplierId, null, null, USER);
        purchaseOrder.confirm();
        purchaseOrder = purchaseOrderRepository.save(purchaseOrder);
        purchaseOrderIds.add(purchaseOrder.getPurchaseOrderId());

        PurchaseOrderLine purchaseOrderLine = purchaseOrderRepository.saveLines(List.of(
                PurchaseOrderLine.register(purchaseOrder.getPurchaseOrderId(), skuId, EXPECTED, BigDecimal.TEN))).get(0);
        purchaseOrderLineIds.add(purchaseOrderLine.getPurchaseOrderLineId());

        Inbound inbound = inboundRepository.save(Inbound.register(
                "IB-IBCONC-" + suffix, purchaseOrder.getPurchaseOrderId(), warehouseId, LocalDateTime.now(), null));
        inboundIds.add(inbound.getInboundId());
        inbound.inspect();
        inbound = inboundRepository.save(inbound);

        inboundRepository.saveLines(List.of(InboundLine.register(
                inbound.getInboundId(), purchaseOrderLine.getPurchaseOrderLineId(), lotId,
                acceptedSectionId, defectSectionId, RECEIVED, ACCEPTED, DEFECTIVE,
                BigDecimal.TEN, null, null, LocalDateTime.now(), USER)));
        return inbound.getInboundId();
    }

    private long quantityOf(Long sectionId, boolean defective) {
        QualityStatus expected = defective ? QualityStatus.DEFECTIVE : QualityStatus.AVAILABLE;
        return inventoryLotJpaRepository.findAll().stream()
                .filter(lot -> sectionId.equals(lot.getSectionId()) && lotId.equals(lot.getLotId()))
                .peek(lot -> assertThat(lot.getQualityStatus()).isEqualTo(expected))
                .mapToLong(InventoryLotJpaEntity::getOnHandQuantity)
                .sum();
    }

    private BigDecimal currentCapacityOf(Long sectionId) {
        return warehouseSectionJpaRepository.findById(sectionId).orElseThrow().getCurrentCapacity();
    }

    /** 모든 작업을 같은 순간에 시작시키고, 작업별 예외(성공이면 null)를 입력 순서대로 돌려준다. */
    private List<Throwable> runConcurrently(List<Callable<Void>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();
        try {
            for (Callable<Void> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        task.call();
                        return null;
                    } catch (Throwable e) {
                        return e;
                    }
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<Throwable> results = new ArrayList<>();
            for (Future<Throwable> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdown();
        }
    }
}

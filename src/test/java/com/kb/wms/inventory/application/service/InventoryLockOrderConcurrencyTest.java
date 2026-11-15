package com.kb.wms.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
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

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.LotJpaRepository;
import com.kb.wms.inventory.application.port.in.InventoryAdjustmentUseCase;
import com.kb.wms.inventory.application.port.in.InventoryStockUseCase;
import com.kb.wms.inventory.application.port.in.command.InventoryAdjustCommand;
import com.kb.wms.inventory.application.port.in.command.StockQuantityCommand;
import com.kb.wms.inventory.application.port.in.command.StockShipCommand;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.InventoryLot;
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
 * 재고 행과 구역 행을 잠그는 순서가 반대인 두 경로가 같은 재고에 동시에 오면 데드락이 나는지 본다
 * ({@code docs/performance/concurrency-locks.md} §4-1).
 *
 * <ul>
 *   <li>출고 피킹({@code ship}): 구역 → 재고 행 (입고 반영과 같은 순서)</li>
 *   <li>재고 조정({@code adjust}): 재고 행 → 구역 (수용량 증감 때문에 재고 행을 잠근 뒤 구역을 잠근다)</li>
 * </ul>
 *
 * <p>두 작업 모두 수량 충돌로 거절될 일이 없게 만들었다. 피킹은 수량을 0으로 두어 보유 수량을 바꾸지 않고,
 * 조정은 100과 101을 번갈아 오간다. 그래서 라운드에서 예외가 나면 락 충돌(데드락·잠금 대기 초과)이다.
 * 기대 수량은 매 라운드 현재 값을 읽어 정한다.
 * 테스트 메서드는 트랜잭션을 걸지 않는다(스레드마다 자기 트랜잭션이 있어야 한다).
 */
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class InventoryLockOrderConcurrencyTest {

    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());
    private static final long USER = 5L;
    private static final int ROUNDS = 100;

    @Autowired private InventoryAdjustmentUseCase adjustmentUseCase;
    @Autowired private InventoryStockUseCase stockUseCase;
    @Autowired private InventoryLotRepository inventoryLotRepository;
    @Autowired private LotRepository lotRepository;
    @Autowired private InventoryLotJpaRepository inventoryLotJpaRepository;
    @Autowired private LotJpaRepository lotJpaRepository;
    @Autowired private ProductSkuJpaRepository productSkuJpaRepository;
    @Autowired private WarehouseJpaRepository warehouseJpaRepository;
    @Autowired private WarehouseSectionJpaRepository warehouseSectionJpaRepository;

    private Long skuId;
    private Long warehouseId;
    private Long sectionId;
    private Long lotId;
    private Long inventoryLotId;

    @BeforeEach
    void setUp() {
        skuId = productSkuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode("SKU-LOCKORD").name("락 순서 테스트 상품").unit("EA")
                .currentPurchasePrice(BigDecimal.ZERO).currentSupplyPrice(BigDecimal.ZERO)
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build()).getSkuId();
        warehouseId = warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode("WH-LOCKORD").name("락 순서 테스트 창고").address("주소")
                .totalCapacity(BigDecimal.valueOf(10000)).status(WarehouseStatus.ACTIVE).build()).getWarehouseId();
        sectionId = warehouseSectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode("LOCKORD-1").name("LOCKORD-1").sectionType("RACK")
                .capacity(BigDecimal.valueOf(1000)).currentCapacity(BigDecimal.valueOf(100))
                .status(WarehouseStatus.ACTIVE).build()).getSectionId();
        lotId = lotRepository.save(Lot.register(skuId, 1L, "LOT-LOCKORD", null, null, BigDecimal.TEN)).getLotId();

        InventoryLot inventoryLot = InventoryLot.open(sectionId, lotId, QualityStatus.AVAILABLE);
        inventoryLot.increase(100L);
        inventoryLotId = inventoryLotRepository.save(inventoryLot).getInventoryLotId();
        // 라운드마다 피킹이 1씩 해제하므로 라운드 수만큼 미리 할당해 둔다.
        stockUseCase.allocate(List.of(new StockQuantityCommand(inventoryLotId, ROUNDS)));
    }

    @AfterEach
    void tearDown() {
        inventoryLotJpaRepository.deleteById(inventoryLotId);
        lotJpaRepository.deleteById(lotId);
        warehouseSectionJpaRepository.deleteById(sectionId);
        warehouseJpaRepository.deleteById(warehouseId);
        productSkuJpaRepository.deleteById(skuId);
    }

    @Test
    @DisplayName("같은 재고에 출고 피킹(구역 → 재고)과 재고 조정(재고 → 구역)이 반복해서 겹쳐도 락 충돌 없이 끝난다")
    void shipAndAdjust_onSameInventory_doNotDeadlock() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<String> failures = new ArrayList<>();
        try {
            for (int round = 0; round < ROUNDS; round++) {
                // 앞 라운드가 실패해도 수량 충돌(STALE_QUANTITY)로 가려지지 않게 매 라운드 현재 수량을 읽는다.
                long before = inventoryLotRepository.findById(inventoryLotId).orElseThrow().getOnHandQuantity();
                long after = before == 100L ? 101L : 100L;
                CyclicBarrier barrier = new CyclicBarrier(2);

                Callable<Void> ship = () -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    stockUseCase.ship(List.of(new StockShipCommand(inventoryLotId, 1L, 0L, 1L, USER)));
                    return null;
                };
                Callable<Void> adjust = () -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    adjustmentUseCase.adjust(new InventoryAdjustCommand(inventoryLotId, before, after, "락 순서 테스트", USER), HQ);
                    return null;
                };

                int currentRound = round;
                Future<Void> shipResult = executor.submit(ship);
                Future<Void> adjustResult = executor.submit(adjust);
                collectFailure(shipResult, "ship", currentRound, failures);
                collectFailure(adjustResult, "adjust", currentRound, failures);
            }
        } finally {
            executor.shutdown();
        }

        InventoryLot result = inventoryLotRepository.findById(inventoryLotId).orElseThrow();
        System.out.println("[LOCK-ORDER] 라운드 " + ROUNDS + "회, 실패 " + failures.size() + "건 " + failures);

        assertThat(failures).as("락 충돌로 실패한 라운드").isEmpty();
        assertThat(result.getAllocatedQuantity()).isZero();
        assertThat(result.getOnHandQuantity()).isBetween(100L, 101L);
    }

    @Test
    @DisplayName("같은 재고에 출고 피킹이 동시에 겹쳐도 보유·할당 수량이 유실되지 않는다")
    void concurrentShip_doesNotLoseUpdates() throws Exception {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        List<Future<Void>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    stockUseCase.ship(List.of(new StockShipCommand(inventoryLotId, 1L, 1L, 1L, USER)));
                    return null;
                }));
            }
            List<String> failures = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                collectFailure(futures.get(i), "ship", i, failures);
            }
            assertThat(failures).as("실패한 피킹").isEmpty();
        } finally {
            executor.shutdown();
        }

        InventoryLot result = inventoryLotRepository.findById(inventoryLotId).orElseThrow();
        System.out.println("[SHIP-STALE] onHand=" + result.getOnHandQuantity() + ", allocated=" + result.getAllocatedQuantity());

        // 시작: 보유 100, 할당 100. 20건이 1씩 피킹하면 둘 다 80이어야 한다.
        assertThat(result.getOnHandQuantity()).as("보유 수량").isEqualTo(80L);
        assertThat(result.getAllocatedQuantity()).as("할당 수량").isEqualTo(80L);
    }

    private void collectFailure(Future<Void> future, String name, int round, List<String> failures) {
        try {
            future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            failures.add(name + "#" + round + ":" + cause.getClass().getSimpleName());
        }
    }
}

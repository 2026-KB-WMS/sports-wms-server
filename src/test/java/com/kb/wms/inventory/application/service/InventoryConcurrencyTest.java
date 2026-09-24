package com.kb.wms.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.LotJpaRepository;
import com.kb.wms.inventory.application.port.in.InventoryAdjustmentUseCase;
import com.kb.wms.inventory.application.port.in.InventoryStockUseCase;
import com.kb.wms.inventory.application.port.in.command.InventoryAdjustCommand;
import com.kb.wms.inventory.application.port.in.command.StockQuantityCommand;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.inventory.exception.InventoryErrorCode;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseSectionJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

/**
 * 같은 InventoryLot 행을 여러 스레드가 동시에 조정·할당할 때 비관적 쓰기 락(SELECT ... FOR UPDATE)이
 * 갱신 유실(lost update)·초과 할당(overselling)을 막는지 검증한다.
 *
 * <p>테스트 메서드 자체는 트랜잭션을 걸지 않는다(각 스레드가 서비스 호출마다 자신만의 트랜잭션·커넥션을 얻어야
 * 실제 동시성이 재현된다). 대신 각 테스트가 만든 행만 {@code @AfterEach}에서 명시적으로 지운다.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class InventoryConcurrencyTest {

    private static final long USER = 5L;

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
                .productId(1L).skuCode("SKU-CONC").name("동시성 테스트 상품").unit("EA")
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build()).getSkuId();
        warehouseId = warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode("WH-CONC").name("동시성 테스트 창고").address("주소").totalCapacity(BigDecimal.valueOf(10000))
                .status(WarehouseStatus.ACTIVE).build()).getWarehouseId();
        sectionId = warehouseSectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode("CONC-1").name("CONC-1").sectionType("RACK")
                .capacity(BigDecimal.valueOf(1000)).currentCapacity(BigDecimal.valueOf(100))
                .status(WarehouseStatus.ACTIVE).build()).getSectionId();
        lotId = lotRepository.save(Lot.register(skuId, 1L, "LOT-CONC", null, null, BigDecimal.TEN)).getLotId();

        InventoryLot inventoryLot = InventoryLot.open(sectionId, lotId, QualityStatus.AVAILABLE);
        inventoryLot.increase(100L);
        inventoryLotId = inventoryLotRepository.save(inventoryLot).getInventoryLotId();
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
    @DisplayName("가용 수량(100)을 초과하는 동시 할당 요청 중 정확히 가용 수량만큼만 성공하고 나머지는 INSUFFICIENT_STOCK으로 거절된다")
    void concurrentAllocate_preventsOverselling() throws Exception {
        int threadCount = 20;
        long quantityPerRequest = 10L;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger insufficientStockCount = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                ready.countDown();
                start.await();
                try {
                    stockUseCase.allocate(List.of(new StockQuantityCommand(inventoryLotId, quantityPerRequest)));
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (InventoryErrorCode.INSUFFICIENT_STOCK.name().equals(e.getErrorCodeName())) {
                        insufficientStockCount.incrementAndGet();
                    } else {
                        throw e;
                    }
                }
                return null;
            });
        }

        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (Callable<Void> task : tasks) {
                futures.add(executor.submit(task));
            }
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            for (Future<Void> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdown();
        }

        InventoryLot result = inventoryLotRepository.findById(inventoryLotId).orElseThrow();

        assertThat(successCount.get()).isEqualTo(10);
        assertThat(insufficientStockCount.get()).isEqualTo(10);
        assertThat(result.getOnHandQuantity()).isEqualTo(100L);
        assertThat(result.getAllocatedQuantity()).isEqualTo(100L);
        assertThat(result.availableQuantity()).isZero();
    }

    @Test
    @DisplayName("같은 재고를 동시에 조정하면 비관적 락으로 직렬화되어, 나중에 잠금을 얻은 쪽은 STALE_QUANTITY로 거절되고 갱신이 유실되지 않는다")
    void concurrentAdjust_pessimisticLock_preventsLostUpdate() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Long> increaseTask = () -> {
            ready.countDown();
            start.await();
            try {
                adjustmentUseCase.adjust(new InventoryAdjustCommand(inventoryLotId, 100L, 150L, "동시 조정 증가", USER));
                return 150L;
            } catch (BusinessException e) {
                assertThat(e.getErrorCodeName()).isEqualTo(InventoryErrorCode.STALE_QUANTITY.name());
                return null;
            }
        };
        Callable<Long> decreaseTask = () -> {
            ready.countDown();
            start.await();
            try {
                adjustmentUseCase.adjust(new InventoryAdjustCommand(inventoryLotId, 100L, 80L, "동시 조정 감소", USER));
                return 80L;
            } catch (BusinessException e) {
                assertThat(e.getErrorCodeName()).isEqualTo(InventoryErrorCode.STALE_QUANTITY.name());
                return null;
            }
        };

        Future<Long> increaseResult = executor.submit(increaseTask);
        Future<Long> decreaseResult = executor.submit(decreaseTask);
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        Long increaseOutcome = increaseResult.get(10, TimeUnit.SECONDS);
        Long decreaseOutcome = decreaseResult.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        List<Long> succeeded = new ArrayList<>();
        if (increaseOutcome != null) {
            succeeded.add(increaseOutcome);
        }
        if (decreaseOutcome != null) {
            succeeded.add(decreaseOutcome);
        }

        InventoryLot result = inventoryLotRepository.findById(inventoryLotId).orElseThrow();

        assertThat(succeeded).hasSize(1);
        assertThat(result.getOnHandQuantity()).isEqualTo(succeeded.get(0));
    }
}

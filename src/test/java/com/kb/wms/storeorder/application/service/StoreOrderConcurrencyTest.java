package com.kb.wms.storeorder.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.adapter.out.persistence.repository.StatusHistoryJpaRepository;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.store.adapter.out.persistence.entity.StoreJpaEntity;
import com.kb.wms.store.adapter.out.persistence.repository.StoreJpaRepository;
import com.kb.wms.store.domain.enums.StoreStatus;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderJpaRepository;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderLineJpaRepository;
import com.kb.wms.storeorder.application.port.in.StoreOrderUseCase;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCancelCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRejectCommand;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 지점 발주 상태 전이의 동시성 검증. 발주 행의 비관적 락이 같은 발주에 대한 동시 승인·반려·취소 중
 * 하나만 성공하게 하는지 본다.
 *
 * <p>테스트 메서드는 트랜잭션을 걸지 않는다(각 스레드가 자신의 트랜잭션·커넥션을 얻어야 실제 동시성이 재현된다).
 * 대신 각 테스트가 만든 행을 {@code @AfterEach}에서 명시적으로 지운다.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class StoreOrderConcurrencyTest {

    private static final long USER = 9L;

    @Autowired private StoreOrderUseCase storeOrderUseCase;
    @Autowired private StoreOrderRepository storeOrderRepository;

    @Autowired private StoreOrderJpaRepository storeOrderJpaRepository;
    @Autowired private StoreOrderLineJpaRepository storeOrderLineJpaRepository;
    @Autowired private StatusHistoryJpaRepository statusHistoryJpaRepository;
    @Autowired private StoreJpaRepository storeJpaRepository;
    @Autowired private ProductSkuJpaRepository productSkuJpaRepository;

    private Long storeId;
    private Long skuId;
    private final List<Long> storeOrderIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        storeId = storeJpaRepository.save(StoreJpaEntity.builder()
                .storeCode("ST-SOCONC").name("발주 동시성 테스트 지점").address("주소").contactNumber("02-1234-5678")
                .status(StoreStatus.ACTIVE).build()).getStoreId();
        skuId = productSkuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode("SKU-SOCONC").name("발주 동시성 테스트 상품").unit("EA")
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build()).getSkuId();
    }

    @AfterEach
    void tearDown() {
        storeOrderIds.forEach(id -> {
            statusHistoryJpaRepository.deleteAll(statusHistoryJpaRepository
                    .findByEntityTypeAndEntityIdOrderByChangedAtAscStatusHistoryIdAsc(
                            StatusHistoryEntityType.STORE_ORDER, id));
            storeOrderLineJpaRepository.deleteAll(storeOrderLineJpaRepository.findAll().stream()
                    .filter(line -> id.equals(line.getStoreOrderId())).toList());
            storeOrderJpaRepository.deleteById(id);
        });
        productSkuJpaRepository.deleteById(skuId);
        storeJpaRepository.deleteById(storeId);
    }

    @Test
    @DisplayName("같은 발주를 동시에 여러 번 승인하면 한 번만 반영되고 나머지는 409로 거절된다")
    void concurrentApprove_sameOrder_appliesOnlyOnce() throws Exception {
        Long orderId = prepareRequestedOrder("A");
        int threadCount = 6;

        List<Throwable> results = runConcurrently(java.util.stream.IntStream.range(0, threadCount)
                .<Callable<Void>>mapToObj(i -> () -> {
                    storeOrderUseCase.approveStoreOrder(orderId, USER);
                    return null;
                })
                .toList());

        assertThat(results.stream().filter(Objects::isNull).count()).isEqualTo(1);
        assertThat(results.stream().filter(Objects::nonNull).toList()).hasSize(threadCount - 1)
                .allSatisfy(e -> {
                    assertThat(e).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) e).getErrorCodeName()).isEqualTo(ErrorCode.CONFLICT.name());
                });
        assertThat(statusOf(orderId)).isEqualTo(StoreOrderStatus.APPROVED);
        assertThat(historyCount(orderId)).as("상태 이력은 성공한 전이 1건만 남는다").isEqualTo(1);
    }

    @Test
    @DisplayName("같은 발주를 동시에 승인·반려·취소하면 하나만 성공하고 최종 상태와 이력이 그 결과와 일치한다")
    void concurrentApproveRejectCancel_sameOrder_onlyOneWins() throws Exception {
        Long orderId = prepareRequestedOrder("B");

        List<Callable<Void>> tasks = List.of(
                () -> {
                    storeOrderUseCase.approveStoreOrder(orderId, USER);
                    return null;
                },
                () -> {
                    storeOrderUseCase.rejectStoreOrder(new StoreOrderRejectCommand(orderId, "반려 사유", USER));
                    return null;
                },
                () -> {
                    // 사유 없는 취소는 승인 전(REQUESTED)에만 가능하다. 승인이 먼저면 400으로 거절된다.
                    storeOrderUseCase.cancelStoreOrder(new StoreOrderCancelCommand(orderId, null, USER));
                    return null;
                });
        List<Throwable> results = runConcurrently(tasks);

        List<Integer> winners = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Throwable result = results.get(i);
            if (result == null) {
                winners.add(i);
            } else {
                assertThat(result).isInstanceOf(BusinessException.class);
                assertThat(((BusinessException) result).getErrorCodeName())
                        .isIn(ErrorCode.CONFLICT.name(), ErrorCode.VALIDATION_ERROR.name());
            }
        }
        assertThat(winners).as("승인·반려·취소 중 정확히 하나만 성공한다").hasSize(1);

        StoreOrderStatus expected = switch (winners.get(0)) {
            case 0 -> StoreOrderStatus.APPROVED;
            case 1 -> StoreOrderStatus.REJECTED;
            case 2 -> StoreOrderStatus.CANCELED;
            default -> throw new IllegalStateException();
        };
        assertThat(statusOf(orderId)).isEqualTo(expected);
        assertThat(historyCount(orderId)).isEqualTo(1);
    }

    /** 승인 대기(REQUESTED) 발주와 항목 1줄을 서비스를 거치지 않고 직접 만든다. */
    private Long prepareRequestedOrder(String suffix) {
        StoreOrder order = storeOrderRepository.save(StoreOrder.builder()
                .orderNo("SO-SOCONC-" + suffix).storeId(storeId).status(StoreOrderStatus.REQUESTED)
                .requestedAt(LocalDateTime.now()).note("동시성").createdBy(USER).build());
        Long id = order.getStoreOrderId();
        storeOrderIds.add(id);
        storeOrderRepository.saveLines(List.of(StoreOrderLine.builder()
                .storeOrderId(id).skuId(skuId).requestedQuantity(10L).shippedQuantity(0L)
                .requestedUnitSupplyPrice(BigDecimal.valueOf(1000))
                .status(StoreOrderLineStatus.REQUESTED).build()));
        return id;
    }

    private StoreOrderStatus statusOf(Long orderId) {
        return storeOrderRepository.findById(orderId).orElseThrow().getStatus();
    }

    private int historyCount(Long orderId) {
        return statusHistoryJpaRepository.findByEntityTypeAndEntityIdOrderByChangedAtAscStatusHistoryIdAsc(
                StatusHistoryEntityType.STORE_ORDER, orderId).size();
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

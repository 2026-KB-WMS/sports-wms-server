package com.kb.wms.outbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.outbound.application.port.in.OutboundUseCase;
import com.kb.wms.outbound.application.port.in.StockAllocationUseCase;
import com.kb.wms.outbound.application.port.in.command.OutboundCancelCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundCreateCommand;
import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;
import com.kb.wms.outbound.application.port.out.OutboundRepository;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.outbound.support.OutboundTestFixture;
import com.kb.wms.outbound.support.OutboundTestFixture.Scenario;

/**
 * 재고 할당·출고 상태 전이의 동시성 검증. 재고 행·발주 행·출고 행의 비관적 락이 초과 할당과 이중 처리를 막는지 본다.
 *
 * <p>테스트 메서드는 트랜잭션을 걸지 않는다(각 스레드가 자신의 트랜잭션·커넥션을 얻어야 실제 동시성이 재현된다).
 * 대신 {@code @AfterEach}에서 관련 테이블을 비운다.
 */
@SpringBootTest
@Import(OutboundTestFixture.class)
@TestPropertySource(properties = "spring.flyway.enabled=false")
class OutboundConcurrencyTest {

    private static final Long USER = 9L;

    @Autowired OutboundTestFixture fixture;
    @Autowired StockAllocationUseCase allocationUseCase;
    @Autowired OutboundUseCase outboundUseCase;
    @Autowired OutboundRepository outboundRepository;
    @Autowired StockAllocationRepository allocationRepository;
    @Autowired InventoryLotJpaRepository inventoryLotJpaRepository;

    @AfterEach
    void tearDown() {
        fixture.cleanUp();
    }

    @Test
    @DisplayName("같은 발주를 동시에 할당하면 한 번만 반영되고 나머지는 거절되며 재고는 초과 할당되지 않는다")
    void concurrentAllocate_sameOrder() throws Exception {
        Scenario s = fixture.create(50, 10);

        List<Throwable> results = runConcurrently(IntStream.range(0, 5)
                .<Callable<Void>>mapToObj(i -> () -> {
                    allocationUseCase.allocate(new StockAllocateCommand(s.orderId(), USER));
                    return null;
                }).toList());

        assertThat(results.stream().filter(Objects::isNull).count()).isEqualTo(1);
        assertThat(errorCodes(results)).containsOnly("ALREADY_ALLOCATED");
        assertThat(allocatedOf(s.inventoryLotId())).isEqualTo(10);
        assertThat(allocationRepository.findByStoreOrderId(s.orderId())).hasSize(1);
    }

    @Test
    @DisplayName("같은 재고를 두 발주가 동시에 요구하면 가용 수량만큼만 할당되고 나머지는 INSUFFICIENT_STOCK")
    void concurrentAllocate_sameStock() throws Exception {
        Scenario s = fixture.create(5, 5);
        Long other = fixture.addOrder(s, 5)[0];

        List<Throwable> results = runConcurrently(List.of(
                () -> {
                    allocationUseCase.allocate(new StockAllocateCommand(s.orderId(), USER));
                    return null;
                },
                () -> {
                    allocationUseCase.allocate(new StockAllocateCommand(other, USER));
                    return null;
                }));

        assertThat(results.stream().filter(Objects::isNull).count()).isEqualTo(1);
        assertThat(errorCodes(results)).containsOnly("INSUFFICIENT_STOCK");
        assertThat(allocatedOf(s.inventoryLotId())).isEqualTo(5);
    }

    @Test
    @DisplayName("같은 READY 출고의 동시 취소·피킹 시작은 한쪽만 성공하고 최종 상태가 그 결과와 일치한다")
    void concurrentCancelAndStartPicking() throws Exception {
        Scenario s = fixture.create(50, 10);
        allocationUseCase.allocate(new StockAllocateCommand(s.orderId(), USER));
        Long outboundId = outboundUseCase.createOutbound(new OutboundCreateCommand(s.orderId(), null, USER))
                .view().outboundId();

        List<Throwable> results = runConcurrently(List.of(
                () -> {
                    outboundUseCase.cancel(new OutboundCancelCommand(outboundId, "취소", USER));
                    return null;
                },
                () -> {
                    outboundUseCase.startPicking(outboundId, USER);
                    return null;
                },
                () -> {
                    outboundUseCase.cancel(new OutboundCancelCommand(outboundId, "취소", USER));
                    return null;
                },
                () -> {
                    outboundUseCase.startPicking(outboundId, USER);
                    return null;
                }));

        assertThat(results.stream().filter(Objects::isNull).count()).isEqualTo(1);
        assertThat(errorCodes(results)).containsOnly("CONFLICT");
        int winner = results.indexOf(null);
        OutboundStatus expected = winner % 2 == 0 ? OutboundStatus.CANCELED : OutboundStatus.PICKING;
        assertThat(outboundRepository.findById(outboundId).orElseThrow().getStatus()).isEqualTo(expected);
    }

    @Test
    @DisplayName("같은 발주의 출고를 동시에 만들면 한 번만 만들어지고 같은 할당이 두 출고에 묶이지 않는다")
    void concurrentCreateOutbound() throws Exception {
        Scenario s = fixture.create(50, 10);
        allocationUseCase.allocate(new StockAllocateCommand(s.orderId(), USER));

        List<Throwable> results = runConcurrently(IntStream.range(0, 4)
                .<Callable<Void>>mapToObj(i -> () -> {
                    outboundUseCase.createOutbound(new OutboundCreateCommand(s.orderId(), null, USER));
                    return null;
                }).toList());

        assertThat(results.stream().filter(Objects::isNull).count()).isEqualTo(1);
        assertThat(errorCodes(results)).containsOnly("NO_ALLOCATION");
        List<Outbound> outbounds = outboundRepository.findByStoreOrderId(s.orderId());
        assertThat(outbounds).hasSize(1);
        assertThat(outboundRepository.findLinesByOutboundId(outbounds.get(0).getOutboundId())).hasSize(1);
    }

    private long allocatedOf(Long inventoryLotId) {
        return inventoryLotJpaRepository.findById(inventoryLotId).orElseThrow().getAllocatedQuantity();
    }

    private List<String> errorCodes(List<Throwable> results) {
        return results.stream().filter(Objects::nonNull).map(e -> {
            assertThat(e).as("예상한 BusinessException이 아님: %s", e).isInstanceOf(BusinessException.class);
            return ((BusinessException) e).getErrorCodeName();
        }).toList();
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

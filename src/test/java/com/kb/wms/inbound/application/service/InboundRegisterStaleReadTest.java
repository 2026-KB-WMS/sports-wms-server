package com.kb.wms.inbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.transaction.support.TransactionTemplate;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.inbound.adapter.out.persistence.repository.InboundJpaRepository;
import com.kb.wms.inbound.adapter.out.persistence.repository.PurchaseOrderJpaRepository;
import com.kb.wms.inbound.adapter.out.persistence.repository.SupplierJpaRepository;
import com.kb.wms.inbound.adapter.out.persistence.repository.SupplierOrderJpaRepository;
import com.kb.wms.inbound.application.port.in.InboundUseCase;
import com.kb.wms.inbound.application.port.in.command.InboundRegisterCommand;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.entity.SupplierOrder;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;
import com.kb.wms.statushistory.adapter.out.persistence.repository.StatusHistoryJpaRepository;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

/**
 * 입고 등록이 공급처 발주서를 락 없이 먼저 읽은 뒤 발주 행 락을 기다리는 사이 발주가 취소되면,
 * 락을 얻은 뒤에도 취소 전 상태로 입고를 등록하는지 본다({@code docs/performance/concurrency-locks.md} §4-1).
 *
 * <p>순서를 고정하려고 테스트가 발주 행 락을 먼저 쥐고, 입고 등록이 그 락 앞에서 기다리는 동안
 * 같은 트랜잭션에서 공급처 발주서를 취소해 커밋한다. 테스트 메서드는 트랜잭션을 걸지 않는다.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class InboundRegisterStaleReadTest {

    private static final AuthenticatedUser ACTOR = new AuthenticatedUser(5L, UserRole.HQ_ADMIN, List.of(), List.of());
    private static final long USER = 5L;

    @Autowired private InboundUseCase inboundUseCase;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private SupplierOrderRepository supplierOrderRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private InboundJpaRepository inboundJpaRepository;
    @Autowired private PurchaseOrderJpaRepository purchaseOrderJpaRepository;
    @Autowired private SupplierJpaRepository supplierJpaRepository;
    @Autowired private SupplierOrderJpaRepository supplierOrderJpaRepository;
    @Autowired private WarehouseJpaRepository warehouseJpaRepository;
    @Autowired private StatusHistoryJpaRepository statusHistoryJpaRepository;

    private Long warehouseId;
    private Long supplierId;
    private Long purchaseOrderId;
    private Long supplierOrderId;

    @BeforeEach
    void setUp() {
        warehouseId = warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode("WH-IBSTALE").name("입고 낡은 읽기 테스트 창고").address("주소")
                .totalCapacity(java.math.BigDecimal.valueOf(1000)).status(WarehouseStatus.ACTIVE).build()).getWarehouseId();
        supplierId = supplierRepository.save(Supplier.register(
                "SUP-IBSTALE", "낡은 읽기 공급처", "담당자", "010-0000-0000", null, null)).getSupplierId();
        PurchaseOrder purchaseOrder = PurchaseOrder.register("PO-IBSTALE", warehouseId, null, null, USER);
        purchaseOrder.confirm();
        purchaseOrderId = purchaseOrderRepository.save(purchaseOrder).getPurchaseOrderId();
        supplierOrderId = supplierOrderRepository.save(
                SupplierOrder.issue("SO-IBSTALE", purchaseOrderId, supplierId)).getSupplierOrderId();
    }

    @AfterEach
    void tearDown() {
        inboundJpaRepository.findAll().stream()
                .filter(inbound -> purchaseOrderId.equals(inbound.getPurchaseOrderId()))
                .forEach(inbound -> {
                    statusHistoryJpaRepository.deleteAll(statusHistoryJpaRepository
                            .findByEntityTypeAndEntityIdOrderByChangedAtAscStatusHistoryIdAsc(
                                    "INBOUND", inbound.getInboundId()));
                    inboundJpaRepository.deleteById(inbound.getInboundId());
                });
        supplierOrderJpaRepository.deleteById(supplierOrderId);
        purchaseOrderJpaRepository.deleteById(purchaseOrderId);
        supplierJpaRepository.deleteById(supplierId);
        warehouseJpaRepository.deleteById(warehouseId);
    }

    @Test
    @DisplayName("입고 등록이 발주 락을 기다리는 사이 공급처 발주서가 취소되면 취소된 발주서에 입고를 만들지 않는다")
    void registerInbound_whileSupplierOrderCanceled_isRejected() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Long> register;
        try {
            register = transactionTemplate.execute(status -> {
                // 발주 행 락을 먼저 쥐어, 입고 등록이 발주서를 읽은 뒤 이 락 앞에서 멈추게 한다.
                purchaseOrderRepository.findByIdForUpdate(purchaseOrderId).orElseThrow();
                Future<Long> future = executor.submit(() -> inboundUseCase.registerInbound(
                        new InboundRegisterCommand(supplierOrderId, null, null, USER), ACTOR));
                sleep(1000);

                SupplierOrder supplierOrder = supplierOrderRepository.findByIdForUpdate(supplierOrderId).orElseThrow();
                supplierOrder.cancel();
                supplierOrderRepository.save(supplierOrder);
                return future;
            });

            List<String> failures = new ArrayList<>();
            try {
                register.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                failures.add((e.getCause() == null ? e : e.getCause()).getClass().getSimpleName());
            }

            long inbounds = inboundJpaRepository.findAll().stream()
                    .filter(inbound -> purchaseOrderId.equals(inbound.getPurchaseOrderId())).count();
            System.out.println("[IB-STALE] 등록 실패=" + failures + ", 만들어진 입고=" + inbounds + ", 발주서 상태="
                    + supplierOrderRepository.findById(supplierOrderId).orElseThrow().getStatus());

            assertThat(supplierOrderRepository.findById(supplierOrderId).orElseThrow().getStatus())
                    .isEqualTo(SupplierOrderStatus.CANCELED);
            assertThat(inbounds).as("취소된 발주서에 만들어진 입고").isZero();
            assertThat(failures).as("입고 등록은 거절돼야 한다").hasSize(1);
        } finally {
            executor.shutdown();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

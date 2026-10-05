package com.kb.wms.outbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryTransactionJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryTransactionJpaRepository;
import com.kb.wms.outbound.application.port.in.OutboundFulfillmentUseCase;
import com.kb.wms.outbound.application.port.in.OutboundUseCase;
import com.kb.wms.outbound.application.port.in.StockAllocationUseCase;
import com.kb.wms.outbound.application.port.in.command.OutboundCreateCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundPickingCompleteCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundPickingCompleteCommand.PickedLine;
import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;
import com.kb.wms.outbound.application.port.in.result.OutboundCreateResult;
import com.kb.wms.outbound.application.port.out.OutboundRepository;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.outbound.support.OutboundTestFixture;
import com.kb.wms.outbound.support.OutboundTestFixture.Scenario;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderJpaRepository;
import com.kb.wms.storeorder.adapter.out.persistence.repository.StoreOrderLineJpaRepository;
import com.kb.wms.storeorder.application.port.in.StoreOrderUseCase;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCancelCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderCancelResult;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderProgressStage;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

import jakarta.persistence.EntityManager;

/**
 * 할당 → 출고 생성 → 피킹 시작·완료 → 배송 → 배송 완료 전체 흐름과, 지점 발주 연동(취소 연쇄·가드)을 실제 서비스로 검증한다.
 * 재고 수량(보유·할당), 재고 이력(InventoryTransaction), 상태 이력(StatusHistory)을 함께 본다.
 */
@SpringBootTest
@Transactional
@Import(OutboundTestFixture.class)
@TestPropertySource(properties = "spring.flyway.enabled=false")
class OutboundFlowIntegrationTest {

    private static final Long USER = 9L;

    @Autowired OutboundTestFixture fixture;
    @Autowired StockAllocationUseCase allocationUseCase;
    @Autowired OutboundUseCase outboundUseCase;
    @Autowired OutboundFulfillmentUseCase fulfillmentUseCase;
    @Autowired StoreOrderUseCase storeOrderUseCase;
    @Autowired StatusHistoryUseCase statusHistoryUseCase;
    @Autowired OutboundRepository outboundRepository;
    @Autowired StockAllocationRepository allocationRepository;
    @Autowired InventoryLotJpaRepository inventoryLotJpaRepository;
    @Autowired InventoryTransactionJpaRepository inventoryTransactionJpaRepository;
    @Autowired StoreOrderJpaRepository storeOrderJpaRepository;
    @Autowired StoreOrderLineJpaRepository storeOrderLineJpaRepository;
    @Autowired EntityManager entityManager;

    private String errorCodeOf(Throwable t) {
        return ((BusinessException) t).getErrorCodeName();
    }

    private InventoryRow inventory(Long inventoryLotId) {
        entityManager.flush();
        entityManager.clear();
        var row = inventoryLotJpaRepository.findById(inventoryLotId).orElseThrow();
        return new InventoryRow(row.getOnHandQuantity(), row.getAllocatedQuantity());
    }

    private record InventoryRow(long onHand, long allocated) {
    }

    private int historyCount(StatusHistoryEntityType type, Long id) {
        return statusHistoryUseCase.findHistory(type, id).size();
    }

    private Long createOutbound(Scenario s) {
        allocationUseCase.allocate(new StockAllocateCommand(s.orderId(), USER));
        return outboundUseCase.createOutbound(new OutboundCreateCommand(s.orderId(), null, USER))
                .view().outboundId();
    }

    // ---------- 전체 흐름 ----------

    @Test
    @DisplayName("전체 흐름: 단계마다 재고 수량·재고 이력·상태 이력이 명세대로 쌓이고 발주가 COMPLETED가 된다")
    void fullFlow() {
        Scenario s = fixture.create(50, 8);

        // 할당: 보유는 그대로, 할당만 늘고 재고 이력은 없다
        OutboundCreateResult created = outboundUseCaseCreate(s);
        Long outboundId = created.view().outboundId();
        assertThat(inventory(s.inventoryLotId())).isEqualTo(new InventoryRow(50, 8));
        assertThat(transactionsOf(outboundId)).isEmpty();

        outboundUseCase.startPicking(outboundId, USER);
        assertThat(inventory(s.inventoryLotId())).isEqualTo(new InventoryRow(50, 8));

        // 피킹 완료: 일부 부족(6/8). 보유 −6, 할당 −8(부족분 예약 해제), 재고 이력 −6
        Long lineId = created.items().get(0).outboundLineId();
        var picked = fulfillmentUseCase.completePicking(new OutboundPickingCompleteCommand(outboundId,
                List.of(new PickedLine(lineId, 6L)), USER));
        assertThat(picked.status()).isEqualTo(OutboundStatus.PICKED);
        assertThat(picked.hasShortage()).isTrue();
        assertThat(inventory(s.inventoryLotId())).isEqualTo(new InventoryRow(44, 0));
        List<InventoryTransactionJpaEntity> transactions = transactionsOf(outboundId);
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getQuantityDelta()).isEqualTo(-6L);
        assertThat(allocationRepository.findByStoreOrderId(s.orderId()))
                .allSatisfy(a -> {
                    assertThat(a.getStatus()).isEqualTo(AllocationStatus.PICKED);
                    assertThat(a.getPickedQuantity()).isEqualTo(6L);
                });

        // 배송 시작·완료: 재고 불변, 부족분이 남아 발주는 ASSIGNED
        fulfillmentUseCase.ship(outboundId, USER);
        assertThat(inventory(s.inventoryLotId())).isEqualTo(new InventoryRow(44, 0));
        var delivered = fulfillmentUseCase.deliver(outboundId, USER);
        assertThat(delivered.storeOrderStatus()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(transactionsOf(outboundId)).hasSize(1);
        assertThat(orderLine(s).getStatus()).isEqualTo(StoreOrderLineStatus.PARTIALLY_SHIPPED);
        assertThat(orderLine(s).getShippedQuantity()).isEqualTo(6L);

        // 후속 할당·출고로 남은 2개 처리
        Long second = createOutboundAgain(s);
        Long secondLine = outboundUseCase.getOutbound(second).items().get(0).outboundLineId();
        outboundUseCase.startPicking(second, USER);
        fulfillmentUseCase.completePicking(new OutboundPickingCompleteCommand(second,
                List.of(new PickedLine(secondLine, 2L)), USER));
        fulfillmentUseCase.ship(second, USER);
        var done = fulfillmentUseCase.deliver(second, USER);

        assertThat(done.storeOrderStatus()).isEqualTo(StoreOrderStatus.COMPLETED);
        assertThat(inventory(s.inventoryLotId())).isEqualTo(new InventoryRow(42, 0));
        assertThat(orderLine(s).getStatus()).isEqualTo(StoreOrderLineStatus.COMPLETED);

        // 상태 이력: 출고 5건(null→READY→PICKING→PICKED→SHIPPED→DELIVERED), 할당 2건(→ALLOCATED, →PICKED)
        assertThat(historyCount(StatusHistoryEntityType.OUTBOUND, outboundId)).isEqualTo(5);
        assertThat(historyCount(StatusHistoryEntityType.OUTBOUND, second)).isEqualTo(5);
        Long firstAllocation = created.items().get(0).allocationId();
        assertThat(historyCount(StatusHistoryEntityType.STOCK_ALLOCATION, firstAllocation)).isEqualTo(2);
        assertThat(historyCount(StatusHistoryEntityType.STORE_ORDER, s.orderId())).isEqualTo(1);
    }

    // ---------- 지점 발주 연동 ----------

    @Test
    @DisplayName("승인 이후 발주 취소는 READY 출고를 취소하고 ALLOCATED 할당을 해제해 재고 할당을 되돌린다")
    void cancelOrderCascades() {
        Scenario s = fixture.create(50, 8);
        Long outboundId = createOutbound(s);
        assertThat(inventory(s.inventoryLotId()).allocated()).isEqualTo(8);

        StoreOrderCancelResult result = storeOrderUseCase.cancelStoreOrder(
                new StoreOrderCancelCommand(s.orderId(), "고객 요청", USER));

        assertThat(result.status()).isEqualTo(StoreOrderStatus.CANCELED);
        assertThat(result.canceledOutboundCount()).isEqualTo(1);
        assertThat(result.releasedAllocationCount()).isEqualTo(1);
        assertThat(outboundRepository.findById(outboundId).orElseThrow().getStatus())
                .isEqualTo(OutboundStatus.CANCELED);
        assertThat(allocationRepository.findByStoreOrderId(s.orderId()))
                .allSatisfy(a -> assertThat(a.getStatus()).isEqualTo(AllocationStatus.RELEASED));
        assertThat(inventory(s.inventoryLotId())).isEqualTo(new InventoryRow(50, 0));
        assertThat(orderLine(s).getAllocatedQuantity()).isZero();
    }

    @Test
    @DisplayName("피킹이 시작된 출고가 있으면 발주를 취소할 수 없다(ORDER_IN_PICKING)")
    void cancelOrderInPicking() {
        Scenario s = fixture.create(50, 8);
        Long outboundId = createOutbound(s);
        outboundUseCase.startPicking(outboundId, USER);

        assertThatThrownBy(() -> storeOrderUseCase.cancelStoreOrder(
                new StoreOrderCancelCommand(s.orderId(), "고객 요청", USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("ORDER_IN_PICKING"));
        assertThat(storeOrderJpaRepository.findById(s.orderId()).orElseThrow().getStatus())
                .isEqualTo(StoreOrderStatus.ASSIGNED);
    }

    @Test
    @DisplayName("진행 중인 출고가 있으면 부분 출고로 종결할 수 없다(OUTBOUND_IN_PROGRESS)")
    void completePartialWithInProgressOutbound() {
        Scenario s = fixture.create(50, 8);
        createOutbound(s);

        assertThatThrownBy(() -> storeOrderUseCase.completePartialStoreOrder(
                new StoreOrderCompletePartialCommand(s.orderId(), "부분 종결", USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("OUTBOUND_IN_PROGRESS"));
    }

    @Test
    @DisplayName("발주 상세는 출고 상태와 진행 단계를 실제 값으로 보여 준다(outbounds, progressStage)")
    void orderDetailShowsOutbound() {
        Scenario s = fixture.create(50, 8);
        assertThat(storeOrderUseCase.getStoreOrderDetails(s.orderId()).outbounds()).isEmpty();
        assertThat(storeOrderUseCase.getStoreOrder(s.orderId()).progressStage())
                .isEqualTo(StoreOrderProgressStage.PREPARING);

        Long outboundId = createOutbound(s);
        assertThat(storeOrderUseCase.getStoreOrderDetails(s.orderId()).outbounds()).hasSize(1)
                .allSatisfy(o -> assertThat(o.status().name()).isEqualTo("READY"));
        outboundUseCase.startPicking(outboundId, USER);
        Long lineId = outboundUseCase.getOutbound(outboundId).items().get(0).outboundLineId();
        fulfillmentUseCase.completePicking(new OutboundPickingCompleteCommand(outboundId,
                List.of(new PickedLine(lineId, 5L)), USER));
        fulfillmentUseCase.ship(outboundId, USER);

        assertThat(storeOrderUseCase.getStoreOrder(s.orderId()).progressStage())
                .isEqualTo(StoreOrderProgressStage.IN_TRANSIT);
        fulfillmentUseCase.deliver(outboundId, USER);
        assertThat(storeOrderUseCase.getStoreOrder(s.orderId()).progressStage())
                .isEqualTo(StoreOrderProgressStage.PARTIALLY_DELIVERED);
    }

    // ---------- 보조 ----------

    private OutboundCreateResult outboundUseCaseCreate(Scenario s) {
        allocationUseCase.allocate(new StockAllocateCommand(s.orderId(), USER));
        return outboundUseCase.createOutbound(new OutboundCreateCommand(s.orderId(), null, USER));
    }

    private Long createOutboundAgain(Scenario s) {
        allocationUseCase.allocate(new StockAllocateCommand(s.orderId(), USER));
        return outboundUseCase.createOutbound(new OutboundCreateCommand(s.orderId(), null, USER))
                .view().outboundId();
    }

    private List<InventoryTransactionJpaEntity> transactionsOf(Long outboundId) {
        entityManager.flush();
        return inventoryTransactionJpaRepository.findAll().stream()
                .filter(t -> outboundId.equals(t.getReferenceId())).toList();
    }

    private com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderLineJpaEntity orderLine(Scenario s) {
        entityManager.flush();
        entityManager.clear();
        return storeOrderLineJpaRepository.findById(s.orderLineId()).orElseThrow();
    }
}

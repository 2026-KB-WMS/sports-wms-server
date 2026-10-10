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

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.LotJpaRepository;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.outbound.application.port.in.OutboundFulfillmentUseCase;
import com.kb.wms.outbound.application.port.in.OutboundUseCase;
import com.kb.wms.outbound.application.port.in.StockAllocationUseCase;
import com.kb.wms.outbound.application.port.in.command.OutboundCreateCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundPickingCompleteCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundPickingCompleteCommand.PickedLine;
import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;
import com.kb.wms.outbound.application.port.in.result.OutboundDeliverResult;
import com.kb.wms.outbound.application.port.in.result.OutboundLineView;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingCompleteResult;
import com.kb.wms.outbound.application.port.in.result.OutboundShipResult;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
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
 * 피킹 완료·배송 시작·배송 완료(서비스 C)를 실제 재고 서비스·발주 연동·상태 이력과 함께 검증한다.
 *
 * <p>데이터: 창고 1개, SKU-A(요청 3, 단가 1000)·SKU-B(요청 2, 단가 500) 항목이 있는 배정(ASSIGNED) 발주,
 * 각 SKU의 보유 50짜리 재고 행 1개. 각 테스트는 할당 → 출고 생성 → 피킹 시작까지 만든 뒤 시작한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class OutboundFulfillmentServiceIntegrationTest {

    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    private static AuthenticatedUser hq(long userId) {
        return new AuthenticatedUser(userId, UserRole.HQ_ADMIN, List.of(), List.of());
    }

    private static final Long USER = 7L;

    @Autowired OutboundFulfillmentUseCase useCase;
    @Autowired OutboundUseCase outboundUseCase;
    @Autowired StockAllocationUseCase allocationUseCase;
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

    Long warehouse;
    Long order;
    Long lotA;
    Long lotB;
    Long lineA;
    Long lineB;

    @BeforeEach
    void setUp() {
        Long store = storeJpaRepository.save(StoreJpaEntity.builder()
                .storeCode("ST-1").name("강남점").address("주소").contactNumber("02-1234-5678")
                .status(StoreStatus.ACTIVE).build()).getStoreId();
        warehouse = warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode("WH-1").name("서울 물류센터").address("주소").totalCapacity(BigDecimal.valueOf(1000))
                .status(WarehouseStatus.ACTIVE).build()).getWarehouseId();
        Long section = sectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouse).sectionCode("A-01").name("A-01").sectionType("STORAGE")
                .capacity(BigDecimal.valueOf(1000)).currentCapacity(BigDecimal.valueOf(100))
                .status(WarehouseStatus.ACTIVE).build()).getSectionId();
        Long skuA = sku("SKU-A");
        Long skuB = sku("SKU-B");
        lotA = inventory(section, skuA, "LOT-A", 50);
        lotB = inventory(section, skuB, "LOT-B", 50);

        order = storeOrderJpaRepository.save(StoreOrderJpaEntity.builder()
                .orderNo("SO-20261005-0001").storeId(store).warehouseId(warehouse)
                .status(StoreOrderStatus.ASSIGNED).requestedAt(LocalDateTime.of(2026, 10, 5, 9, 0))
                .createdBy(1L).build()).getStoreOrderId();
        lineA = line(order, skuA, 3, new BigDecimal("1000"));
        lineB = line(order, skuB, 2, new BigDecimal("500"));
    }

    private Long sku(String code) {
        return skuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode(code).name(code).unit("EA")
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build()).getSkuId();
    }

    private Long inventory(Long sectionId, Long skuId, String lotNumber, long onHand) {
        Long lotId = lotJpaRepository.save(LotJpaEntity.builder()
                .skuId(skuId).supplierId(1L).lotNumber(lotNumber).expiryDate(LocalDate.of(2026, 12, 31))
                .status(LotStatus.AVAILABLE).unitCost(BigDecimal.TEN).build()).getLotId();
        return inventoryLotJpaRepository.save(InventoryLotJpaEntity.builder()
                .sectionId(sectionId).lotId(lotId).onHandQuantity(onHand).allocatedQuantity(0L)
                .qualityStatus(QualityStatus.AVAILABLE).build()).getInventoryLotId();
    }

    private Long line(Long orderId, Long skuId, long requested, BigDecimal price) {
        return storeOrderLineJpaRepository.save(StoreOrderLineJpaEntity.builder()
                .storeOrderId(orderId).skuId(skuId).requestedQuantity(requested).allocatedQuantity(0L)
                .shippedQuantity(0L).requestedUnitSupplyPrice(price)
                .status(StoreOrderLineStatus.REQUESTED).build()).getStoreOrderLineId();
    }

    /** 할당 → 출고 생성 → 피킹 시작까지 만들고 출고 ID를 돌려준다. */
    private Long pickingOutbound() {
        allocationUseCase.allocate(new StockAllocateCommand(order, USER), HQ);
        Long outboundId = outboundUseCase.createOutbound(new OutboundCreateCommand(order, null, USER), HQ)
                .view().outboundId();
        outboundUseCase.startPicking(outboundId, hq(USER));
        return outboundId;
    }

    private OutboundLineView lineOf(Long outboundId, Long skuCodeLot) {
        return outboundUseCase.getOutbound(outboundId, HQ).items().stream()
                .filter(i -> i.inventoryLotId().equals(skuCodeLot)).findFirst().orElseThrow();
    }

    private OutboundPickingCompleteCommand pick(Long outboundId, long pickedA, long pickedB) {
        return new OutboundPickingCompleteCommand(outboundId, List.of(
                new PickedLine(lineOf(outboundId, lotA).outboundLineId(), pickedA),
                new PickedLine(lineOf(outboundId, lotB).outboundLineId(), pickedB)), USER);
    }

    private InventoryLotJpaEntity inventoryRow(Long inventoryLotId) {
        entityManager.flush();
        entityManager.clear();
        return inventoryLotJpaRepository.findById(inventoryLotId).orElseThrow();
    }

    private StoreOrderLineJpaEntity orderLine(Long lineId) {
        entityManager.flush();
        entityManager.clear();
        return storeOrderLineJpaRepository.findById(lineId).orElseThrow();
    }

    private String errorCodeOf(Throwable t) {
        return ((BusinessException) t).getErrorCodeName();
    }

    @Test
    @DisplayName("다른 창고 관리자는 피킹 완료·배송 시작·배송 완료가 403이고 재고·상태를 바꾸지 않는다")
    void otherWarehouseManager_forbidden() {
        Long outboundId = pickingOutbound();
        AuthenticatedUser other = new AuthenticatedUser(8L, UserRole.WAREHOUSE_MANAGER, List.of(-1L), List.of());

        assertThatThrownBy(() -> useCase.completePicking(pick(outboundId, 3, 2), other))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(() -> useCase.ship(outboundId, other))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(() -> useCase.deliver(outboundId, other))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("FORBIDDEN"));
        assertThat(inventoryRow(lotA).getOnHandQuantity()).isEqualTo(50);
    }

    // ---------- 피킹 완료 ----------

    @Test
    @DisplayName("전량 피킹하면 재고가 차감되고 할당·발주 항목 수량과 출고 상태가 함께 바뀐다")
    void completePicking() {
        Long outboundId = pickingOutbound();

        OutboundPickingCompleteResult result = useCase.completePicking(pick(outboundId, 3, 2), HQ);

        assertThat(result.status()).isEqualTo(OutboundStatus.PICKED);
        assertThat(result.hasShortage()).isFalse();
        assertThat(result.items()).hasSize(2);
        assertThat(result.items()).allSatisfy(i -> assertThat(i.shortageQuantity()).isZero());
        assertThat(result.items().stream().map(i -> i.lineAmount().intValue()).toList())
                .containsExactlyInAnyOrder(3000, 1000);

        assertThat(inventoryRow(lotA).getOnHandQuantity()).isEqualTo(47);
        assertThat(inventoryRow(lotA).getAllocatedQuantity()).isZero();
        assertThat(inventoryRow(lotB).getOnHandQuantity()).isEqualTo(48);
        StoreOrderLineJpaEntity a = orderLine(lineA);
        assertThat(a.getAllocatedQuantity()).isZero();
        assertThat(a.getShippedQuantity()).isEqualTo(3);
        assertThat(a.getStatus()).isNotEqualTo(StoreOrderLineStatus.COMPLETED); // 항목 상태는 배송 완료에서 바뀐다
        assertThat(statusHistoryUseCase.findHistory(StatusHistoryEntityType.OUTBOUND, outboundId)).hasSize(3);
    }

    @Test
    @DisplayName("부족분이 있으면 예약만 풀리고 발주 항목에는 미출고 수량이 남는다")
    void completePickingWithShortage() {
        Long outboundId = pickingOutbound();

        OutboundPickingCompleteResult result = useCase.completePicking(pick(outboundId, 1, 2), HQ);

        assertThat(result.hasShortage()).isTrue();
        assertThat(result.items().stream().mapToLong(i -> i.shortageQuantity()).sum()).isEqualTo(2);
        assertThat(inventoryRow(lotA).getOnHandQuantity()).isEqualTo(49);
        assertThat(inventoryRow(lotA).getAllocatedQuantity()).isZero();
        StoreOrderLineJpaEntity a = orderLine(lineA);
        assertThat(a.getAllocatedQuantity()).isZero();
        assertThat(a.getShippedQuantity()).isEqualTo(1);
        // 남은 2개는 다시 할당할 수 있다
        assertThat(allocationUseCase.allocate(new StockAllocateCommand(order, USER), HQ).items()).hasSize(1);
    }

    @Test
    @DisplayName("전 항목이 0이면 NOTHING_PICKED이고 아무것도 반영되지 않는다")
    void nothingPicked() {
        Long outboundId = pickingOutbound();

        assertThatThrownBy(() -> useCase.completePicking(pick(outboundId, 0, 0), HQ))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("NOTHING_PICKED"));
        assertThat(outboundUseCase.getOutbound(outboundId, HQ).view().status()).isEqualTo(OutboundStatus.PICKING);
    }

    @Test
    @DisplayName("공급 단가가 발주 항목에도 SKU에도 없으면 SUPPLY_PRICE_MISSING")
    void supplyPriceMissing() {
        Long outboundId = pickingOutbound();
        StoreOrderLineJpaEntity entity = storeOrderLineJpaRepository.findById(lineA).orElseThrow();
        storeOrderLineJpaRepository.save(StoreOrderLineJpaEntity.builder()
                .storeOrderLineId(entity.getStoreOrderLineId()).storeOrderId(order).skuId(entity.getSkuId())
                .requestedQuantity(3L).allocatedQuantity(entity.getAllocatedQuantity())
                .shippedQuantity(0L).requestedUnitSupplyPrice(null)
                .status(StoreOrderLineStatus.REQUESTED).build());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> useCase.completePicking(pick(outboundId, 3, 2), HQ))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("SUPPLY_PRICE_MISSING"));
    }

    @Test
    @DisplayName("피킹 완료 입력 검증: 개수·ID 불일치, 중복, 음수, 할당 수량 초과, 사용자 필수")
    void completePickingInvalid() {
        Long outboundId = pickingOutbound();
        Long lineIdA = lineOf(outboundId, lotA).outboundLineId();
        Long lineIdB = lineOf(outboundId, lotB).outboundLineId();

        assertThatThrownBy(() -> useCase.completePicking(new OutboundPickingCompleteCommand(outboundId, null, USER), HQ))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.completePicking(new OutboundPickingCompleteCommand(
                outboundId, List.of(new PickedLine(lineIdA, 3L)), USER), HQ))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.completePicking(new OutboundPickingCompleteCommand(
                outboundId, List.of(new PickedLine(lineIdA, 3L), new PickedLine(lineIdA, 3L)), USER), HQ))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.completePicking(new OutboundPickingCompleteCommand(
                outboundId, List.of(new PickedLine(lineIdA, 3L), new PickedLine(999_999L, 2L)), USER), HQ))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.completePicking(new OutboundPickingCompleteCommand(
                outboundId, List.of(new PickedLine(lineIdA, -1L), new PickedLine(lineIdB, 2L)), USER), HQ))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.completePicking(pick(outboundId, 4, 2), HQ))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.completePicking(new OutboundPickingCompleteCommand(
                outboundId, List.of(), null), HQ))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThat(inventoryRow(lotA).getOnHandQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("PICKING이 아니거나 없는 출고는 피킹을 완료할 수 없다")
    void completePickingConflict() {
        allocationUseCase.allocate(new StockAllocateCommand(order, USER), HQ);
        Long outboundId = outboundUseCase.createOutbound(new OutboundCreateCommand(order, null, USER), HQ)
                .view().outboundId();

        assertThatThrownBy(() -> useCase.completePicking(new OutboundPickingCompleteCommand(outboundId, List.of(), USER), HQ))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("CONFLICT"));
        assertThatThrownBy(() -> useCase.completePicking(new OutboundPickingCompleteCommand(999_999L, List.of(), USER), HQ))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("OUTBOUND_NOT_FOUND"));
    }

    // ---------- 배송 시작 ----------

    @Test
    @DisplayName("PICKED 출고만 배송을 시작하고 재고는 바뀌지 않는다")
    void ship() {
        Long outboundId = pickingOutbound();
        assertThatThrownBy(() -> useCase.ship(outboundId, hq(USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("CONFLICT"));
        useCase.completePicking(pick(outboundId, 3, 2), HQ);

        OutboundShipResult result = useCase.ship(outboundId, hq(USER));

        assertThat(result.status()).isEqualTo(OutboundStatus.SHIPPED);
        assertThat(result.shippedAt()).isNotNull();
        assertThat(result.shippedBy()).isEqualTo(USER);
        assertThat(inventoryRow(lotA).getOnHandQuantity()).isEqualTo(47);
        assertThatThrownBy(() -> useCase.ship(outboundId, hq(USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("CONFLICT"));
    }

    // ---------- 배송 완료 ----------

    @Test
    @DisplayName("전량 출고된 배송 완료는 항목을 COMPLETED로, 발주를 COMPLETED로 바꾼다")
    void deliverCompletesOrder() {
        Long outboundId = pickingOutbound();
        useCase.completePicking(pick(outboundId, 3, 2), HQ);
        useCase.ship(outboundId, hq(USER));

        OutboundDeliverResult result = useCase.deliver(outboundId, hq(USER));

        assertThat(result.status()).isEqualTo(OutboundStatus.DELIVERED);
        assertThat(result.deliveredAt()).isNotNull();
        assertThat(result.storeOrderStatus()).isEqualTo(StoreOrderStatus.COMPLETED);
        assertThat(orderLine(lineA).getStatus()).isEqualTo(StoreOrderLineStatus.COMPLETED);
        assertThat(orderLine(lineB).getStatus()).isEqualTo(StoreOrderLineStatus.COMPLETED);
        assertThat(statusHistoryUseCase.findHistory(StatusHistoryEntityType.STORE_ORDER, order)).isNotEmpty();
        assertThatThrownBy(() -> useCase.deliver(outboundId, hq(USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("CONFLICT"));
    }

    @Test
    @DisplayName("부족분이 있으면 항목은 PARTIALLY_SHIPPED, 발주는 ASSIGNED를 유지하고 후속 출고로 마무리된다")
    void deliverPartialThenFollowUp() {
        Long first = pickingOutbound();
        useCase.completePicking(pick(first, 1, 2), HQ);
        useCase.ship(first, hq(USER));

        OutboundDeliverResult partial = useCase.deliver(first, hq(USER));

        assertThat(partial.storeOrderStatus()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(orderLine(lineA).getStatus()).isEqualTo(StoreOrderLineStatus.PARTIALLY_SHIPPED);
        assertThat(orderLine(lineB).getStatus()).isEqualTo(StoreOrderLineStatus.COMPLETED);

        // 후속 할당·출고
        allocationUseCase.allocate(new StockAllocateCommand(order, USER), HQ);
        Long second = outboundUseCase.createOutbound(new OutboundCreateCommand(order, null, USER), HQ)
                .view().outboundId();
        outboundUseCase.startPicking(second, hq(USER));
        OutboundLineView only = outboundUseCase.getOutbound(second, HQ).items().get(0);
        useCase.completePicking(new OutboundPickingCompleteCommand(second,
                List.of(new PickedLine(only.outboundLineId(), 2L)), USER), HQ);
        useCase.ship(second, hq(USER));

        OutboundDeliverResult done = useCase.deliver(second, hq(USER));

        assertThat(done.storeOrderStatus()).isEqualTo(StoreOrderStatus.COMPLETED);
        assertThat(orderLine(lineA).getStatus()).isEqualTo(StoreOrderLineStatus.COMPLETED);
    }

    @Test
    @DisplayName("진행 중인 다른 출고가 있으면 전량 출고돼도 발주는 ASSIGNED를 유지한다")
    void deliverWithOtherOutboundInProgress() {
        // 발주 항목 A만 먼저 전량 처리하는 출고, B는 별도 출고를 READY로 남긴다.
        allocationUseCase.allocate(new StockAllocateCommand(order, USER), HQ);
        Long all = outboundUseCase.createOutbound(new OutboundCreateCommand(order, null, USER), HQ)
                .view().outboundId();
        outboundUseCase.startPicking(all, hq(USER));
        useCase.completePicking(pick(all, 3, 2), HQ);
        useCase.ship(all, hq(USER));
        // 다른 진행 중 출고가 생기려면 할당이 더 있어야 하므로 항목을 늘려 준비한다.
        Long skuC = sku("SKU-C");
        inventory(inventoryLotJpaRepository.findById(lotA).orElseThrow().getSectionId(), skuC, "LOT-C", 10);
        line(order, skuC, 1, new BigDecimal("1000"));
        allocationUseCase.allocate(new StockAllocateCommand(order, USER), HQ);
        Long other = outboundUseCase.createOutbound(new OutboundCreateCommand(order, null, USER), HQ)
                .view().outboundId();

        OutboundDeliverResult result = useCase.deliver(all, hq(USER));

        assertThat(other).isNotEqualTo(all);
        assertThat(result.storeOrderStatus()).isEqualTo(StoreOrderStatus.ASSIGNED);
    }
}

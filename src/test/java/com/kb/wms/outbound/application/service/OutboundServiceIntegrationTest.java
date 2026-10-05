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

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.repository.LotJpaRepository;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.outbound.application.port.in.OutboundUseCase;
import com.kb.wms.outbound.application.port.in.StockAllocationUseCase;
import com.kb.wms.outbound.application.port.in.command.OutboundCancelCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundCreateCommand;
import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;
import com.kb.wms.outbound.application.port.in.command.StockAllocationReleaseCommand;
import com.kb.wms.outbound.application.port.in.query.OutboundSearchCondition;
import com.kb.wms.outbound.application.port.in.result.OutboundCancelResult;
import com.kb.wms.outbound.application.port.in.result.OutboundCreateResult;
import com.kb.wms.outbound.application.port.in.result.OutboundDetail;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingStartResult;
import com.kb.wms.outbound.application.port.in.result.OutboundSummary;
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

/**
 * 출고 생성·피킹 시작·취소·조회(서비스 B)를 실제 할당 서비스·발주 연동·상태 이력과 함께 검증한다.
 *
 * <p>데이터: 창고 1개, SKU-A(요청 3)·SKU-B(요청 2) 항목이 있는 배정(ASSIGNED) 발주와 각 SKU의 충분한 재고.
 * 각 테스트는 필요한 시점에 {@link StockAllocationUseCase}로 할당을 만든다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class OutboundServiceIntegrationTest {

    private static final Long USER = 7L;

    @Autowired OutboundUseCase useCase;
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

    Long store;
    Long warehouse;
    Long order;

    @BeforeEach
    void setUp() {
        store = storeJpaRepository.save(StoreJpaEntity.builder()
                .storeCode("ST-1").name("강남점").address("주소").contactNumber("02-1234-5678")
                .status(StoreStatus.ACTIVE).build()).getStoreId();
        warehouse = warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode("WH-1").name("서울 물류센터").address("주소").totalCapacity(BigDecimal.valueOf(1000))
                .status(WarehouseStatus.ACTIVE).build()).getWarehouseId();
        Long section = sectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouse).sectionCode("A-01").name("A-01").sectionType("STORAGE")
                .capacity(BigDecimal.valueOf(1000)).currentCapacity(BigDecimal.ZERO)
                .status(WarehouseStatus.ACTIVE).build()).getSectionId();
        Long skuA = sku("SKU-A");
        Long skuB = sku("SKU-B");
        inventory(section, skuA, "LOT-A", 50);
        inventory(section, skuB, "LOT-B", 50);

        order = storeOrderJpaRepository.save(StoreOrderJpaEntity.builder()
                .orderNo("SO-20261005-0001").storeId(store).warehouseId(warehouse)
                .status(StoreOrderStatus.ASSIGNED).requestedAt(LocalDateTime.of(2026, 10, 5, 9, 0))
                .createdBy(1L).build()).getStoreOrderId();
        line(order, skuA, 3);
        line(order, skuB, 2);
    }

    private Long sku(String code) {
        return skuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode(code).name(code).unit("EA")
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build()).getSkuId();
    }

    private void inventory(Long sectionId, Long skuId, String lotNumber, long onHand) {
        Long lotId = lotJpaRepository.save(LotJpaEntity.builder()
                .skuId(skuId).supplierId(1L).lotNumber(lotNumber).expiryDate(LocalDate.of(2026, 12, 31))
                .status(LotStatus.AVAILABLE).unitCost(BigDecimal.TEN).build()).getLotId();
        inventoryLotJpaRepository.save(InventoryLotJpaEntity.builder()
                .sectionId(sectionId).lotId(lotId).onHandQuantity(onHand).allocatedQuantity(0L)
                .qualityStatus(QualityStatus.AVAILABLE).build());
    }

    private void line(Long orderId, Long skuId, long requested) {
        storeOrderLineJpaRepository.save(StoreOrderLineJpaEntity.builder()
                .storeOrderId(orderId).skuId(skuId).requestedQuantity(requested).allocatedQuantity(0L)
                .shippedQuantity(0L).requestedUnitSupplyPrice(new BigDecimal("1000"))
                .status(StoreOrderLineStatus.REQUESTED).build());
    }

    private List<Long> allocate() {
        return allocationUseCase.allocate(new StockAllocateCommand(order, USER)).items().stream()
                .map(i -> i.allocationId()).toList();
    }

    private String errorCodeOf(Throwable t) {
        return ((BusinessException) t).getErrorCodeName();
    }

    // ---------- 생성 ----------

    @Test
    @DisplayName("ALLOCATED 할당 전부를 READY 출고로 묶고 상태 이력을 남긴다")
    void create() {
        allocate();

        OutboundCreateResult result = useCase.createOutbound(new OutboundCreateCommand(order, " 오전 출고 ", USER));

        assertThat(result.view().status()).isEqualTo(OutboundStatus.READY);
        assertThat(result.view().outboundNo()).matches("OB-\\d{8}-0001");
        assertThat(result.view().note()).isEqualTo("오전 출고");
        assertThat(result.view().storeName()).isEqualTo("강남점");
        assertThat(result.lineCount()).isEqualTo(2);
        assertThat(statusHistoryUseCase.findHistory(StatusHistoryEntityType.OUTBOUND,
                result.view().outboundId())).hasSize(1);
    }

    @Test
    @DisplayName("출고 번호는 당일 일련번호로 증가한다")
    void createSequence() {
        List<Long> ids = allocate();
        useCase.createOutbound(new OutboundCreateCommand(order, null, USER));
        // 첫 출고를 취소해 할당을 다시 묶을 수 있게 한다
        Long first = useCase.searchOutbounds(emptyCondition()).get(0).outboundId();
        useCase.cancel(new OutboundCancelCommand(first, "재작업", USER));

        OutboundCreateResult second = useCase.createOutbound(new OutboundCreateCommand(order, null, USER));

        assertThat(ids).hasSize(2);
        assertThat(second.view().outboundNo()).endsWith("-0002");
    }

    @Test
    @DisplayName("이미 출고에 묶인 할당만 남았으면 NO_ALLOCATION")
    void createNoAllocation() {
        assertThatThrownBy(() -> useCase.createOutbound(new OutboundCreateCommand(order, null, USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("NO_ALLOCATION"));

        allocate();
        useCase.createOutbound(new OutboundCreateCommand(order, null, USER));
        assertThatThrownBy(() -> useCase.createOutbound(new OutboundCreateCommand(order, null, USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("NO_ALLOCATION"));
    }

    @Test
    @DisplayName("생성 입력 검증: 발주·사용자 필수, 비고 500자 이하, 없는 발주는 404")
    void createInvalid() {
        allocate();

        assertThatThrownBy(() -> useCase.createOutbound(new OutboundCreateCommand(null, null, USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.createOutbound(new OutboundCreateCommand(order, null, null)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.createOutbound(
                new OutboundCreateCommand(order, "가".repeat(501), USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.createOutbound(new OutboundCreateCommand(999_999L, null, USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("STORE_ORDER_NOT_FOUND"));
        assertThat(useCase.createOutbound(
                new OutboundCreateCommand(order, "가".repeat(500), USER)).view().note()).hasSize(500);
    }

    // ---------- 피킹 시작 ----------

    @Test
    @DisplayName("READY 출고는 PICKING으로 바뀌고, 이후 할당 해제와 재시작은 막힌다")
    void startPicking() {
        List<Long> ids = allocate();
        Long outboundId = useCase.createOutbound(new OutboundCreateCommand(order, null, USER))
                .view().outboundId();

        OutboundPickingStartResult result = useCase.startPicking(outboundId, USER);

        assertThat(result.status()).isEqualTo(OutboundStatus.PICKING);
        assertThat(statusHistoryUseCase.findHistory(StatusHistoryEntityType.OUTBOUND, outboundId)).hasSize(2);
        assertThatThrownBy(() -> useCase.startPicking(outboundId, USER))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("CONFLICT"));
        assertThatThrownBy(() -> allocationUseCase.release(
                new StockAllocationReleaseCommand(ids.get(0), "사유", USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("ALLOCATION_IN_OUTBOUND"));
    }

    @Test
    @DisplayName("피킹 시작 입력 검증: 없는 출고는 404, 사용자 필수")
    void startPickingInvalid() {
        assertThatThrownBy(() -> useCase.startPicking(999_999L, USER))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("OUTBOUND_NOT_FOUND"));
        assertThatThrownBy(() -> useCase.startPicking(1L, null))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.startPicking(null, USER))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
    }

    // ---------- 취소 ----------

    @Test
    @DisplayName("READY 출고를 취소하면 할당은 유지되고 사유가 이력에 남는다")
    void cancel() {
        allocate();
        Long outboundId = useCase.createOutbound(new OutboundCreateCommand(order, null, USER))
                .view().outboundId();

        OutboundCancelResult result = useCase.cancel(new OutboundCancelCommand(outboundId, " 오배정 ", USER));

        assertThat(result.status()).isEqualTo(OutboundStatus.CANCELED);
        assertThat(result.cancelReason()).isEqualTo("오배정");
        OutboundDetail detail = useCase.getOutbound(outboundId);
        assertThat(detail.cancelReason()).isEqualTo("오배정");
        assertThat(detail.items()).hasSize(2);
    }

    @Test
    @DisplayName("PICKING 출고나 이미 취소된 출고는 취소할 수 없다(CONFLICT)")
    void cancelConflict() {
        allocate();
        Long outboundId = useCase.createOutbound(new OutboundCreateCommand(order, null, USER))
                .view().outboundId();
        useCase.startPicking(outboundId, USER);

        assertThatThrownBy(() -> useCase.cancel(new OutboundCancelCommand(outboundId, "사유", USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("CONFLICT"));
    }

    @Test
    @DisplayName("취소 입력 검증: 사유 필수·500자 이하, 사용자 필수, 없는 출고는 404")
    void cancelInvalid() {
        assertThatThrownBy(() -> useCase.cancel(new OutboundCancelCommand(1L, "  ", USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.cancel(new OutboundCancelCommand(1L, "가".repeat(501), USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.cancel(new OutboundCancelCommand(1L, "사유", null)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> useCase.cancel(new OutboundCancelCommand(999_999L, "사유", USER)))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("OUTBOUND_NOT_FOUND"));
    }

    // ---------- 조회 ----------

    @Test
    @DisplayName("목록은 상태·발주 필터를 적용하고 기간 역전은 검증 오류")
    void search() {
        allocate();
        useCase.createOutbound(new OutboundCreateCommand(order, null, USER));

        List<OutboundSummary> ready = useCase.searchOutbounds(new OutboundSearchCondition(
                OutboundStatus.READY, null, null, order, null, null, null));
        List<OutboundSummary> picking = useCase.searchOutbounds(new OutboundSearchCondition(
                OutboundStatus.PICKING, null, null, null, null, null, null));

        assertThat(ready).hasSize(1);
        assertThat(picking).isEmpty();
        LocalDateTime now = LocalDateTime.now();
        assertThatThrownBy(() -> useCase.searchOutbounds(new OutboundSearchCondition(
                null, null, null, null, null, now, now.minusDays(1))))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("상세 조회는 항목과 로트·구역을 담고 없는 출고는 404")
    void get() {
        allocate();
        Long outboundId = useCase.createOutbound(new OutboundCreateCommand(order, null, USER))
                .view().outboundId();

        OutboundDetail detail = useCase.getOutbound(outboundId);

        assertThat(detail.items()).hasSize(2);
        assertThat(detail.items()).allSatisfy(i -> {
            assertThat(i.sectionCode()).isEqualTo("A-01");
            assertThat(i.lotNumber()).isNotNull();
        });
        assertThat(detail.cancelReason()).isNull();
        assertThatThrownBy(() -> useCase.getOutbound(999_999L))
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo("OUTBOUND_NOT_FOUND"));
    }

    private OutboundSearchCondition emptyCondition() {
        return new OutboundSearchCondition(null, null, null, null, null, null, null);
    }
}

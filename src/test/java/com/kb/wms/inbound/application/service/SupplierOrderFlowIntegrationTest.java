package com.kb.wms.inbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.inbound.application.port.in.InboundCompleteUseCase;
import com.kb.wms.inbound.application.port.in.InboundInspectUseCase;
import com.kb.wms.inbound.application.port.in.InboundUseCase;
import com.kb.wms.inbound.application.port.in.PurchaseOrderUseCase;
import com.kb.wms.inbound.application.port.in.SupplierUseCase;
import com.kb.wms.inbound.application.port.in.command.InboundCancelCommand;
import com.kb.wms.inbound.application.port.in.command.InboundInspectCommand;
import com.kb.wms.inbound.application.port.in.command.InboundRegisterCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderCancelCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderConfirmCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRegisterCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRejectCommand;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderConfirmResult;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SkuSupplierRepository;
import com.kb.wms.inbound.application.port.out.SupplierOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.entity.SkuSupplier;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.entity.SupplierOrder;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;
import com.kb.wms.inbound.exception.InboundErrorCode;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inbound.exception.SupplierErrorCode;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseSectionJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

/**
 * 창고 발주 요청 → 공급처 발주서 → 입고 흐름의 통합 시나리오(ADR-030). 서비스를 실제 저장소와 함께 호출해
 * 확정 시 발주서 분리, 발주서 단위 병렬 입고, 완료·취소 전파, 공급처 비활성화 가드를 확인한다.
 *
 * <p>데이터: 창고 1개(합격 구역 + 불량 구역), SKU 2개(A: 공급처 1·2에 매핑, B: 공급처 2에 매핑), 공급처 2곳.
 * 매입 단가는 모두 1,000이다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class SupplierOrderFlowIntegrationTest {

    private static final BigDecimal PRICE = BigDecimal.valueOf(1000);

    @Autowired PurchaseOrderUseCase purchaseOrderUseCase;
    @Autowired InboundUseCase inboundUseCase;
    @Autowired InboundInspectUseCase inboundInspectUseCase;
    @Autowired InboundCompleteUseCase inboundCompleteUseCase;
    @Autowired SupplierUseCase supplierUseCase;
    @Autowired PurchaseOrderRepository purchaseOrderRepository;
    @Autowired SupplierOrderRepository supplierOrderRepository;
    @Autowired SkuSupplierRepository skuSupplierRepository;
    @Autowired SupplierRepository supplierRepository;
    @Autowired WarehouseJpaRepository warehouseJpaRepository;
    @Autowired WarehouseSectionJpaRepository warehouseSectionJpaRepository;
    @Autowired ProductSkuJpaRepository productSkuJpaRepository;

    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    private AuthenticatedUser manager;
    private Long warehouseId;
    private Long acceptedSectionId;
    private Long defectSectionId;
    private Long skuA;
    private Long skuB;
    private Long supplier1;
    private Long supplier2;
    private int lotSequence;

    @BeforeEach
    void setUp() {
        warehouseId = warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode("WH-FLOW").name("흐름 검증 창고").address("주소")
                .totalCapacity(BigDecimal.valueOf(100000)).status(WarehouseStatus.ACTIVE).build()).getWarehouseId();
        acceptedSectionId = section("FLOW-A", "RACK");
        defectSectionId = section("FLOW-D", "DEFECT");
        manager = new AuthenticatedUser(5L, UserRole.WAREHOUSE_MANAGER, List.of(warehouseId), List.of());

        skuA = sku("SKU-FLOW-A");
        skuB = sku("SKU-FLOW-B");
        supplier1 = supplierRepository.save(
                Supplier.register("SUP-FLOW-1", "공급처 1", "담당자", "02-1111-1111", null, null)).getSupplierId();
        supplier2 = supplierRepository.save(
                Supplier.register("SUP-FLOW-2", "공급처 2", "담당자", "02-2222-2222", null, null)).getSupplierId();
        skuSupplierRepository.save(SkuSupplier.register(skuA, supplier1));
        skuSupplierRepository.save(SkuSupplier.register(skuA, supplier2));
        skuSupplierRepository.save(SkuSupplier.register(skuB, supplier2));
    }

    // ---------- 시나리오 ----------

    @Test
    @DisplayName("한 요청에 공급처가 다른 SKU를 담아 확정하면 공급처별 발주서가 만들어지고, 두 발주서를 병렬로 입고해 모두 끝나면 요청이 완료된다")
    void twoSuppliers_parallelInbounds_completeRequest() {
        Long poId = requestOrder(skuA, 10, skuB, 6);
        Long lineA = lineOf(poId, skuA);
        Long lineB = lineOf(poId, skuB);

        PurchaseOrderConfirmResult confirmed = purchaseOrderUseCase.confirmPurchaseOrder(poId,
                new PurchaseOrderConfirmCommand(1L, List.of(
                        new PurchaseOrderConfirmCommand.Line(lineA, supplier1),
                        new PurchaseOrderConfirmCommand.Line(lineB, supplier2))));

        assertThat(confirmed.purchaseOrder().getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        assertThat(confirmed.supplierOrders()).hasSize(2);
        assertThat(confirmed.supplierOrders()).extracting(s -> s.supplierId())
                .containsExactlyInAnyOrder(supplier1, supplier2);
        Long so1 = supplierOrderOf(poId, supplier1);
        Long so2 = supplierOrderOf(poId, supplier2);
        assertThat(purchaseOrderRepository.findLineById(lineA).orElseThrow().getSupplierOrderId()).isEqualTo(so1);
        assertThat(purchaseOrderRepository.findLineById(lineB).orElseThrow().getSupplierOrderId()).isEqualTo(so2);

        // 두 발주서에 각각 입고를 등록한다. 한 발주서의 진행 중 입고가 다른 발주서를 막지 않는다.
        Long inbound1 = inboundUseCase.registerInbound(new InboundRegisterCommand(so1, null, null, 5L), manager);
        Long inbound2 = inboundUseCase.registerInbound(new InboundRegisterCommand(so2, null, null, 5L), manager);

        inspect(inbound1, lineA, 10);
        inboundCompleteUseCase.completeInbound(inbound1, manager);
        assertThat(supplierOrder(so1).getStatus()).isEqualTo(SupplierOrderStatus.COMPLETED);
        assertThat(supplierOrder(so2).getStatus()).isEqualTo(SupplierOrderStatus.ISSUED);
        assertThat(purchaseOrder(poId).getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);

        inspect(inbound2, lineB, 6);
        inboundCompleteUseCase.completeInbound(inbound2, manager);
        assertThat(supplierOrder(so2).getStatus()).isEqualTo(SupplierOrderStatus.COMPLETED);
        assertThat(purchaseOrder(poId).getStatus()).isEqualTo(PurchaseOrderStatus.COMPLETED);
        assertThat(purchaseOrderRepository.findLineById(lineA).orElseThrow().getStatus())
                .isEqualTo(PurchaseOrderLineStatus.COMPLETED);
    }

    @Test
    @DisplayName("같은 공급처의 SKU는 발주서 한 건으로 묶이고, 발주서 단위 진행 중 입고는 한 건만 허용된다")
    void sameSupplier_oneSupplierOrder_singleInboundInProgress() {
        Long poId = requestOrder(skuA, 10, skuB, 6);
        Long lineA = lineOf(poId, skuA);
        Long lineB = lineOf(poId, skuB);

        PurchaseOrderConfirmResult confirmed = purchaseOrderUseCase.confirmPurchaseOrder(poId,
                new PurchaseOrderConfirmCommand(1L, List.of(
                        new PurchaseOrderConfirmCommand.Line(lineA, supplier2),
                        new PurchaseOrderConfirmCommand.Line(lineB, supplier2))));

        assertThat(confirmed.supplierOrders()).hasSize(1);
        Long so = supplierOrderOf(poId, supplier2);
        assertThat(confirmed.supplierOrders().get(0).lineCount()).isEqualTo(2L);

        Long first = inboundUseCase.registerInbound(new InboundRegisterCommand(so, null, null, 5L), manager);
        assertError(() -> inboundUseCase.registerInbound(new InboundRegisterCommand(so, null, null, 5L), manager),
                InboundErrorCode.INBOUND_IN_PROGRESS.name());

        // 부분 입고(A 4개)를 끝내면 발주서는 PARTIALLY_RECEIVED가 되고 다음 입고를 받을 수 있다.
        inspect(first, lineA, 4);
        inboundCompleteUseCase.completeInbound(first, manager);
        assertThat(supplierOrder(so).getStatus()).isEqualTo(SupplierOrderStatus.PARTIALLY_RECEIVED);
        assertThat(purchaseOrder(poId).getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);

        Long second = inboundUseCase.registerInbound(new InboundRegisterCommand(so, null, null, 5L), manager);
        assertThat(second).isNotEqualTo(first);
        inspectAll(second, new long[] {lineA, 6}, new long[] {lineB, 6});
        inboundCompleteUseCase.completeInbound(second, manager);
        assertThat(supplierOrder(so).getStatus()).isEqualTo(SupplierOrderStatus.COMPLETED);
        assertThat(purchaseOrder(poId).getStatus()).isEqualTo(PurchaseOrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("다른 공급처 발주서의 항목은 이 입고에서 검수할 수 없다")
    void inspect_lineOfOtherSupplierOrder_rejected() {
        Long poId = requestOrder(skuA, 10, skuB, 6);
        Long lineA = lineOf(poId, skuA);
        Long lineB = lineOf(poId, skuB);
        purchaseOrderUseCase.confirmPurchaseOrder(poId, new PurchaseOrderConfirmCommand(1L, List.of(
                new PurchaseOrderConfirmCommand.Line(lineA, supplier1),
                new PurchaseOrderConfirmCommand.Line(lineB, supplier2))));
        Long inboundOfSupplier1 = inboundUseCase.registerInbound(
                new InboundRegisterCommand(supplierOrderOf(poId, supplier1), null, null, 5L), manager);

        assertError(() -> inspect(inboundOfSupplier1, lineB, 6), ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("취소되지 않은 입고가 있으면 요청을 취소할 수 없고, 입고를 취소하면 요청 취소가 발주서로 전파되며 입고 등록이 막힌다")
    void cancel_blockedByInbound_thenCascadesToSupplierOrders() {
        Long poId = requestOrder(skuA, 10, skuB, 6);
        Long lineA = lineOf(poId, skuA);
        Long lineB = lineOf(poId, skuB);
        purchaseOrderUseCase.confirmPurchaseOrder(poId, new PurchaseOrderConfirmCommand(1L, List.of(
                new PurchaseOrderConfirmCommand.Line(lineA, supplier1),
                new PurchaseOrderConfirmCommand.Line(lineB, supplier2))));
        Long so1 = supplierOrderOf(poId, supplier1);
        Long so2 = supplierOrderOf(poId, supplier2);
        Long inbound = inboundUseCase.registerInbound(new InboundRegisterCommand(so1, null, null, 5L), manager);

        assertError(() -> purchaseOrderUseCase.cancelPurchaseOrder(
                        poId, new PurchaseOrderCancelCommand("공급 불가", 1L), HQ),
                PurchaseOrderErrorCode.PURCHASE_ORDER_HAS_INBOUND.name());
        assertThat(purchaseOrder(poId).getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        assertThat(supplierOrder(so1).getStatus()).isEqualTo(SupplierOrderStatus.ISSUED);

        inboundUseCase.cancelInbound(inbound, new InboundCancelCommand("착오 등록", 5L), manager);
        purchaseOrderUseCase.cancelPurchaseOrder(poId, new PurchaseOrderCancelCommand("공급 불가", 1L), HQ);

        assertThat(purchaseOrder(poId).getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
        assertThat(supplierOrder(so1).getStatus()).isEqualTo(SupplierOrderStatus.CANCELED);
        assertThat(supplierOrder(so2).getStatus()).isEqualTo(SupplierOrderStatus.CANCELED);
        assertError(() -> inboundUseCase.registerInbound(new InboundRegisterCommand(so2, null, null, 5L), manager),
                ErrorCode.CONFLICT.name());
    }

    @Test
    @DisplayName("반려된 요청에는 발주서가 없고 다시 확정·반려할 수 없다")
    void reject_noSupplierOrders_andFinal() {
        Long poId = requestOrder(skuA, 10);

        purchaseOrderUseCase.rejectPurchaseOrder(poId, new PurchaseOrderRejectCommand("수량 과다", 1L), HQ);

        assertThat(purchaseOrder(poId).getStatus()).isEqualTo(PurchaseOrderStatus.REJECTED);
        assertThat(supplierOrderRepository.findByPurchaseOrderId(poId)).isEmpty();
        assertError(() -> purchaseOrderUseCase.confirmPurchaseOrder(poId,
                        new PurchaseOrderConfirmCommand(1L, List.of(
                                new PurchaseOrderConfirmCommand.Line(lineOf(poId, skuA), supplier1)))),
                ErrorCode.CONFLICT.name());
        assertError(() -> purchaseOrderUseCase.rejectPurchaseOrder(
                poId, new PurchaseOrderRejectCommand("다시", 1L), HQ), ErrorCode.CONFLICT.name());
    }

    @Test
    @DisplayName("확정 검증에 실패하면 발주서가 하나도 만들어지지 않고 요청은 REQUESTED로 남는다")
    void confirm_failure_isAtomic() {
        Long poId = requestOrder(skuA, 10, skuB, 6);
        Long lineA = lineOf(poId, skuA);
        Long lineB = lineOf(poId, skuB);

        // 공급처 1은 SKU B에 매핑되어 있지 않다
        assertError(() -> purchaseOrderUseCase.confirmPurchaseOrder(poId,
                        new PurchaseOrderConfirmCommand(1L, List.of(
                                new PurchaseOrderConfirmCommand.Line(lineA, supplier1),
                                new PurchaseOrderConfirmCommand.Line(lineB, supplier1)))),
                PurchaseOrderErrorCode.SKU_SUPPLIER_NOT_MAPPED.name());
        // 한 라인만 지정하면 공급처 없는 라인이 남는다
        assertError(() -> purchaseOrderUseCase.confirmPurchaseOrder(poId,
                        new PurchaseOrderConfirmCommand(1L, List.of(
                                new PurchaseOrderConfirmCommand.Line(lineA, supplier1)))),
                PurchaseOrderErrorCode.LINE_SUPPLIER_REQUIRED.name());

        assertThat(purchaseOrder(poId).getStatus()).isEqualTo(PurchaseOrderStatus.REQUESTED);
        assertThat(supplierOrderRepository.findByPurchaseOrderId(poId)).isEmpty();
        assertThat(purchaseOrderRepository.findLineById(lineA).orElseThrow().getSupplierId()).isNull();
    }

    @Test
    @DisplayName("진행 중인 공급처 발주서가 있는 공급처는 비활성화할 수 없고, 발주서가 끝나면 비활성화할 수 있다")
    void supplierDeactivation_guardedByInProgressSupplierOrders() {
        Long poId = requestOrder(skuB, 6);
        Long lineB = lineOf(poId, skuB);
        purchaseOrderUseCase.confirmPurchaseOrder(poId, new PurchaseOrderConfirmCommand(1L, List.of(
                new PurchaseOrderConfirmCommand.Line(lineB, supplier2))));
        Long so = supplierOrderOf(poId, supplier2);

        assertError(() -> supplierUseCase.deactivateSupplier(supplier2), SupplierErrorCode.SUPPLIER_IN_USE.name());

        Long inbound = inboundUseCase.registerInbound(new InboundRegisterCommand(so, null, null, 5L), manager);
        inspect(inbound, lineB, 6);
        inboundCompleteUseCase.completeInbound(inbound, manager);

        assertThat(supplierUseCase.deactivateSupplier(supplier2).isActive()).isFalse();
    }

    @Test
    @DisplayName("완료된 입고의 상태와 발주서·요청 상태는 서로 어긋나지 않는다")
    void completedInbound_statesAreConsistent() {
        Long poId = requestOrder(skuB, 6);
        Long lineB = lineOf(poId, skuB);
        purchaseOrderUseCase.confirmPurchaseOrder(poId, new PurchaseOrderConfirmCommand(1L, List.of(
                new PurchaseOrderConfirmCommand.Line(lineB, supplier2))));
        Long so = supplierOrderOf(poId, supplier2);
        Long inbound = inboundUseCase.registerInbound(new InboundRegisterCommand(so, null, null, 5L), manager);
        inspect(inbound, lineB, 6);

        var result = inboundCompleteUseCase.completeInbound(inbound, manager);

        assertThat(result.inbound().getStatus()).isEqualTo(InboundStatus.COMPLETED);
        assertThat(result.supplierOrderId()).isEqualTo(so);
        assertThat(result.supplierOrderStatus()).isEqualTo(SupplierOrderStatus.COMPLETED);
        assertThat(result.purchaseOrderStatus()).isEqualTo(PurchaseOrderStatus.COMPLETED);
    }

    // ---------- fixtures ----------

    private static void assertError(ThrowingCallable call, String errorCodeName) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(errorCodeName);
    }

    private Long section(String code, String type) {
        return warehouseSectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode(code).name(code).sectionType(type)
                .capacity(BigDecimal.valueOf(100000)).currentCapacity(BigDecimal.ZERO)
                .status(WarehouseStatus.ACTIVE).build()).getSectionId();
    }

    private Long sku(String code) {
        return productSkuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode(code).name(code).unit("EA")
                .currentPurchasePrice(PRICE).currentSupplyPrice(BigDecimal.valueOf(1500))
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE).build()).getSkuId();
    }

    /** 창고 관리자가 SKU·수량만 요청한다. 인자는 (skuId, 수량) 쌍의 나열이다. */
    private Long requestOrder(Object... skuAndQuantity) {
        List<PurchaseOrderRegisterCommand.Line> lines = new java.util.ArrayList<>();
        for (int i = 0; i < skuAndQuantity.length; i += 2) {
            lines.add(new PurchaseOrderRegisterCommand.Line(
                    (Long) skuAndQuantity[i], ((Number) skuAndQuantity[i + 1]).longValue()));
        }
        return purchaseOrderUseCase.registerPurchaseOrder(
                new PurchaseOrderRegisterCommand(warehouseId, null, null, 5L, lines), manager);
    }

    private Long lineOf(Long purchaseOrderId, Long skuId) {
        return purchaseOrderRepository.findLinesByPurchaseOrderId(purchaseOrderId).stream()
                .filter(line -> line.getSkuId().equals(skuId))
                .map(PurchaseOrderLine::getPurchaseOrderLineId)
                .findFirst().orElseThrow();
    }

    private Long supplierOrderOf(Long purchaseOrderId, Long supplierId) {
        return supplierOrderRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .filter(order -> order.getSupplierId().equals(supplierId))
                .map(SupplierOrder::getSupplierOrderId)
                .findFirst().orElseThrow();
    }

    private SupplierOrder supplierOrder(Long supplierOrderId) {
        return supplierOrderRepository.findById(supplierOrderId).orElseThrow();
    }

    private PurchaseOrder purchaseOrder(Long purchaseOrderId) {
        return purchaseOrderRepository.findById(purchaseOrderId).orElseThrow();
    }

    /** 발주 항목 한 줄을 전량 합격으로 검수한다. */
    private void inspect(Long inboundId, Long purchaseOrderLineId, long quantity) {
        inspectAll(inboundId, new long[] {purchaseOrderLineId, quantity});
    }

    private void inspectAll(Long inboundId, long[]... lineAndQuantity) {
        List<InboundInspectCommand.Line> lines = new java.util.ArrayList<>();
        for (long[] entry : lineAndQuantity) {
            lines.add(new InboundInspectCommand.Line(
                    entry[0], "LOT-FLOW-" + (++lotSequence), null, null, entry[1], entry[1], 0L, PRICE, null, null,
                    acceptedSectionId, defectSectionId));
        }
        inboundInspectUseCase.inspectInbound(inboundId, new InboundInspectCommand(5L, lines), manager);
    }
}

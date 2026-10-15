package com.kb.wms.inbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderCancelCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderConfirmCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRegisterCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRejectCommand;
import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderConfirmResult;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderDetails;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSupplierView;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderQueryRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SkuPurchasePricePort;
import com.kb.wms.inbound.application.port.out.SkuSupplierRepository;
import com.kb.wms.inbound.application.port.out.SupplierOrderQueryRepository;
import com.kb.wms.inbound.application.port.out.SupplierOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.application.port.out.WarehouseAvailabilityPort;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.entity.SkuSupplier;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.entity.SupplierOrder;
import com.kb.wms.inbound.domain.enums.InboundHistoryEntityType;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inbound.exception.SupplierErrorCode;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    // 발주 5번 사용자가 만든 요청 발주의 작성자(창고 관리자)
    private static final AuthenticatedUser AUTHOR =
            new AuthenticatedUser(5L, UserRole.WAREHOUSE_MANAGER, List.of(1L), List.of());
    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderQueryRepository purchaseOrderQueryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SkuSupplierRepository skuSupplierRepository;

    @Mock
    private SupplierOrderRepository supplierOrderRepository;

    @Mock
    private SupplierOrderQueryRepository supplierOrderQueryRepository;

    @Mock
    private WarehouseAvailabilityPort warehouseAvailabilityPort;

    @Mock
    private SkuPurchasePricePort skuPurchasePricePort;

    @Mock
    private InboundRepository inboundRepository;

    @Mock
    private StatusHistoryUseCase statusHistoryUseCase;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    // ---------- fixtures ----------

    private static Supplier activeSupplier() {
        return Supplier.register("SUP-001", "공급처 A", "박담당", "02-555-1234", null, null);
    }

    private static Supplier inactiveSupplier() {
        Supplier supplier = activeSupplier();
        supplier.deactivate();
        return supplier;
    }

    private static PurchaseOrder purchaseOrder(Long id, PurchaseOrderStatus status) {
        return PurchaseOrder.builder()
                .purchaseOrderId(id)
                .purchaseOrderNo("PO-20261002-0001")
                .warehouseId(1L)
                .status(status)
                .createdBy(5L)
                .build();
    }

    private static PurchaseOrderRegisterCommand registerCommand(LocalDateTime expectedAt, String note,
                                                                PurchaseOrderRegisterCommand.Line... lines) {
        return new PurchaseOrderRegisterCommand(1L, expectedAt, note, 5L, List.of(lines));
    }

    private static PurchaseOrderRegisterCommand.Line line(Long skuId, long quantity) {
        return new PurchaseOrderRegisterCommand.Line(skuId, quantity);
    }

    private static void assertError(ThrowingCallable call, String errorCodeName) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(errorCodeName);
    }

    // ---------- 목록 필터 ----------

    @Test
    @DisplayName("발주 목록의 warehouseId 필터가 존재하지 않으면 창고 도메인의 404를 그대로 던지고 조회하지 않는다")
    void getPurchaseOrders_unknownWarehouse_notFound() {
        doThrow(new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND))
                .when(warehouseAvailabilityPort).requireExists(999L);

        assertError(() -> purchaseOrderService.getPurchaseOrders(
                PurchaseOrderSearchCondition.unscoped(null, 999L, null, null, null, null), HQ),
                WarehouseErrorCode.WAREHOUSE_NOT_FOUND.name());
        verifyNoInteractions(purchaseOrderQueryRepository);
    }

    @Test
    @DisplayName("발주 목록의 supplierId 필터가 존재하지 않으면 SUPPLIER_NOT_FOUND 404를 던지고 조회하지 않는다")
    void getPurchaseOrders_unknownSupplier_notFound() {
        when(supplierRepository.existsById(999L)).thenReturn(false);

        assertError(() -> purchaseOrderService.getPurchaseOrders(
                PurchaseOrderSearchCondition.unscoped(null, null, 999L, null, null, null), HQ),
                SupplierErrorCode.SUPPLIER_NOT_FOUND.name());
        verifyNoInteractions(purchaseOrderQueryRepository);
    }

    @Test
    @DisplayName("발주 목록은 비담당 창고를 지정하면 존재 여부를 확인하기 전에 403이다")
    void getPurchaseOrders_forbiddenBeforeExistenceCheck() {
        AuthenticatedUser manager = new AuthenticatedUser(5L, UserRole.WAREHOUSE_MANAGER, List.of(1L), List.of());

        assertError(() -> purchaseOrderService.getPurchaseOrders(
                PurchaseOrderSearchCondition.unscoped(null, 3L, null, null, null, null), manager),
                ErrorCode.FORBIDDEN.name());
        verifyNoInteractions(warehouseAvailabilityPort, purchaseOrderQueryRepository);
    }

    private static PurchaseOrderView viewWithStatus(PurchaseOrderStatus status) {
        return new PurchaseOrderView(
                4L, "PO-20261002-0001", 1L, "서울 물류센터", status,
                null, null, 1L, BigDecimal.valueOf(6000000), 5L, null, null, null);
    }

    /** 저장 시 발주 ID를 채워 돌려주는 목 동작 */
    private void stubSaveAssigningId(Long id) {
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            PurchaseOrder source = invocation.getArgument(0);
            return PurchaseOrder.builder()
                    .purchaseOrderId(id)
                    .purchaseOrderNo(source.getPurchaseOrderNo())
                    .warehouseId(source.getWarehouseId())
                    .status(source.getStatus())
                    .expectedAt(source.getExpectedAt())
                    .note(source.getNote())
                    .createdBy(source.getCreatedBy())
                    .build();
        });
    }

    // ---------- 등록 ----------

    @Test
    @DisplayName("발주를 등록하면 REQUESTED 발주와 매입 단가 스냅샷이 담긴 항목을 한 번에 저장하고 발주 ID를 반환한다")
    void register_success() {
        LocalDateTime expectedAt = LocalDateTime.now().plusDays(3);
        when(skuPurchasePricePort.getPurchasablePrice(1L)).thenReturn(BigDecimal.valueOf(60000));
        when(skuPurchasePricePort.getPurchasablePrice(2L)).thenReturn(BigDecimal.valueOf(5000));
        when(purchaseOrderRepository.countByPurchaseOrderNoPrefix(anyString())).thenReturn(2L);
        stubSaveAssigningId(4L);

        Long id = purchaseOrderService.registerPurchaseOrder(
                registerCommand(expectedAt, "정기 보충 발주", line(1L, 100), line(2L, 10)), HQ);

        assertThat(id).isEqualTo(4L);
        verify(warehouseAvailabilityPort).requireActive(1L);

        ArgumentCaptor<PurchaseOrder> headerCaptor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepository).save(headerCaptor.capture());
        PurchaseOrder header = headerCaptor.getValue();
        assertThat(header.getStatus()).isEqualTo(PurchaseOrderStatus.REQUESTED);
        assertThat(header.getWarehouseId()).isEqualTo(1L);
        assertThat(header.getCreatedBy()).isEqualTo(5L);
        assertThat(header.getNote()).isEqualTo("정기 보충 발주");
        assertThat(header.getExpectedAt()).isEqualTo(expectedAt);
        assertThat(header.getPurchaseOrderNo()).matches("PO-\\d{8}-0003");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PurchaseOrderLine>> linesCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseOrderRepository).saveLines(linesCaptor.capture());
        List<PurchaseOrderLine> lines = linesCaptor.getValue();
        assertThat(lines).hasSize(2);
        assertThat(lines).allSatisfy(saved -> {
            assertThat(saved.getPurchaseOrderId()).isEqualTo(4L);
            assertThat(saved.getStatus()).isEqualTo(PurchaseOrderLineStatus.REQUESTED);
            assertThat(saved.getReceivedQuantity()).isZero();
        });
        assertThat(lines.get(0).getSkuId()).isEqualTo(1L);
        assertThat(lines.get(0).getExpectedQuantity()).isEqualTo(100L);
        assertThat(lines.get(0).getOrderedUnitPrice()).isEqualByComparingTo("60000");
        assertThat(lines.get(0).getLineAmount()).isEqualByComparingTo("6000000");
        assertThat(lines.get(1).getSkuId()).isEqualTo(2L);
        assertThat(lines.get(1).getLineAmount()).isEqualByComparingTo("50000");

        verify(statusHistoryUseCase).record(
                InboundHistoryEntityType.PURCHASE_ORDER.name(), 4L, null, "REQUESTED", null, 5L);
    }

    @Test
    @DisplayName("입고 예정 일시와 비고 없이도 등록할 수 있고 당일 첫 발주 번호는 0001이다")
    void register_withoutOptionalFields() {
        when(skuPurchasePricePort.getPurchasablePrice(1L)).thenReturn(BigDecimal.valueOf(60000));
        when(purchaseOrderRepository.countByPurchaseOrderNoPrefix(anyString())).thenReturn(0L);
        stubSaveAssigningId(4L);

        purchaseOrderService.registerPurchaseOrder(registerCommand(null, null, line(1L, 1)), HQ);

        ArgumentCaptor<PurchaseOrder> headerCaptor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepository).save(headerCaptor.capture());
        assertThat(headerCaptor.getValue().getPurchaseOrderNo()).matches("PO-\\d{8}-0001");
        assertThat(headerCaptor.getValue().getExpectedAt()).isNull();
        assertThat(headerCaptor.getValue().getNote()).isNull();
    }

    @Test
    @DisplayName("발주 항목이 비어 있으면 VALIDATION_ERROR를 던진다")
    void register_emptyLines() {
        assertError(() -> purchaseOrderService.registerPurchaseOrder(registerCommand(null, null), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, warehouseAvailabilityPort, skuPurchasePricePort);
    }

    @Test
    @DisplayName("발주 수량이 0 이하면 VALIDATION_ERROR를 던진다")
    void register_nonPositiveQuantity() {
        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 0)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, warehouseAvailabilityPort, skuPurchasePricePort);
    }

    @Test
    @DisplayName("같은 SKU를 두 번 넣으면 VALIDATION_ERROR를 던진다")
    void register_duplicateSku() {
        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 10), line(1L, 20)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, warehouseAvailabilityPort, skuPurchasePricePort);
    }

    @Test
    @DisplayName("입고 예정 일시가 과거면 VALIDATION_ERROR를 던진다")
    void register_expectedAtInPast() {
        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(LocalDateTime.now().minusMinutes(1), null, line(1L, 10)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, warehouseAvailabilityPort, skuPurchasePricePort);
    }

    @Test
    @DisplayName("비고가 1000자를 넘으면 VALIDATION_ERROR를 던진다")
    void register_noteTooLong() {
        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, "가".repeat(1001), line(1L, 10)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, warehouseAvailabilityPort, skuPurchasePricePort);
    }

    @Test
    @DisplayName("창고가 비활성이면 창고 포트의 예외가 그대로 전달되고 저장하지 않는다")
    void register_warehouseInactive() {
        doThrow(new BusinessException(ErrorCode.CONFLICT, "비활성 창고에는 발주를 등록할 수 없습니다."))
                .when(warehouseAvailabilityPort).requireActive(1L);

        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 10)), HQ),
                ErrorCode.CONFLICT.name());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("SKU에 매입 단가가 없으면 PURCHASE_PRICE_MISSING이 전달되고 저장하지 않는다")
    void register_purchasePriceMissing() {
        when(skuPurchasePricePort.getPurchasablePrice(1L))
                .thenThrow(new BusinessException(PurchaseOrderErrorCode.PURCHASE_PRICE_MISSING));

        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 10)), HQ),
                PurchaseOrderErrorCode.PURCHASE_PRICE_MISSING.name());
        verify(purchaseOrderRepository, never()).save(any());
        verify(purchaseOrderRepository, never()).saveLines(any());
    }

    // ---------- 확정 ----------

    private static PurchaseOrderLine requestedLine(Long id, Long skuId, Long supplierId) {
        return PurchaseOrderLine.builder()
                .purchaseOrderLineId(id)
                .purchaseOrderId(4L)
                .skuId(skuId)
                .supplierId(supplierId)
                .expectedQuantity(10L)
                .orderedUnitPrice(BigDecimal.valueOf(1000))
                .lineAmount(BigDecimal.valueOf(10000))
                .status(PurchaseOrderLineStatus.REQUESTED)
                .build();
    }

    private static SkuSupplier mapping(Long skuId, Long supplierId) {
        return SkuSupplier.register(skuId, supplierId);
    }

    private void givenRequestedOrder(PurchaseOrderLine... lines) {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.REQUESTED)));
        when(purchaseOrderRepository.findLinesByPurchaseOrderIdForUpdate(4L)).thenReturn(List.of(lines));
    }

    private void stubConfirmSaves() {
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AtomicLong ids = new AtomicLong(100L);
        when(supplierOrderRepository.save(any(SupplierOrder.class))).thenAnswer(invocation -> {
            SupplierOrder source = invocation.getArgument(0);
            return SupplierOrder.builder()
                    .supplierOrderId(ids.incrementAndGet())
                    .supplierOrderNo(source.getSupplierOrderNo())
                    .purchaseOrderId(source.getPurchaseOrderId())
                    .supplierId(source.getSupplierId())
                    .status(source.getStatus())
                    .build();
        });
        when(purchaseOrderRepository.saveLines(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("공급처를 지정해 확정하면 공급처별로 발주서를 한 건씩 만들고 라인을 연결한 뒤 CONFIRMED로 저장한다")
    void confirm_success_createsSupplierOrderPerSupplier() {
        givenRequestedOrder(requestedLine(11L, 1L, null), requestedLine(12L, 2L, null), requestedLine(13L, 3L, null));
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(activeSupplier()));
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(activeSupplier()));
        when(skuSupplierRepository.findBySkuIdIn(any())).thenReturn(
                List.of(mapping(1L, 3L), mapping(2L, 3L), mapping(3L, 7L)));
        when(supplierOrderRepository.countBySupplierOrderNoPrefix(anyString())).thenReturn(1L);
        stubConfirmSaves();

        PurchaseOrderConfirmResult result = purchaseOrderService.confirmPurchaseOrder(4L,
                new PurchaseOrderConfirmCommand(5L, List.of(
                        new PurchaseOrderConfirmCommand.Line(11L, 3L),
                        new PurchaseOrderConfirmCommand.Line(12L, 3L),
                        new PurchaseOrderConfirmCommand.Line(13L, 7L))));

        assertThat(result.purchaseOrder().getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);

        ArgumentCaptor<SupplierOrder> orders = ArgumentCaptor.forClass(SupplierOrder.class);
        verify(supplierOrderRepository, org.mockito.Mockito.times(2)).save(orders.capture());
        assertThat(orders.getAllValues()).extracting(SupplierOrder::getSupplierId).containsExactly(3L, 7L);
        assertThat(orders.getAllValues()).extracting(SupplierOrder::getSupplierOrderNo)
                .satisfies(numbers -> {
                    assertThat(numbers.get(0)).matches("SO-\\d{8}-0002");
                    assertThat(numbers.get(1)).matches("SO-\\d{8}-0003");
                });
        assertThat(orders.getAllValues()).allSatisfy(order -> {
            assertThat(order.getPurchaseOrderId()).isEqualTo(4L);
            assertThat(order.getStatus()).isEqualTo(SupplierOrderStatus.ISSUED);
        });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PurchaseOrderLine>> lines = ArgumentCaptor.forClass(List.class);
        verify(purchaseOrderRepository).saveLines(lines.capture());
        assertThat(lines.getValue()).extracting(PurchaseOrderLine::getPurchaseOrderLineId)
                .containsExactly(11L, 12L, 13L);
        assertThat(lines.getValue()).extracting(PurchaseOrderLine::getSupplierId).containsExactly(3L, 3L, 7L);
        assertThat(lines.getValue()).extracting(PurchaseOrderLine::getSupplierOrderId)
                .containsExactly(101L, 101L, 102L);

        verify(statusHistoryUseCase).record(
                InboundHistoryEntityType.SUPPLIER_ORDER.name(), 101L, null, "ISSUED", null, 5L);
        verify(statusHistoryUseCase).record(
                InboundHistoryEntityType.SUPPLIER_ORDER.name(), 102L, null, "ISSUED", null, 5L);
        verify(statusHistoryUseCase).record(
                InboundHistoryEntityType.PURCHASE_ORDER.name(), 4L, "REQUESTED", "CONFIRMED", null, 5L);
    }

    @Test
    @DisplayName("이미 배정된 공급처는 요청에 지정하지 않아도 유지되고, 같은 공급처 라인은 발주서 한 건으로 묶인다")
    void confirm_keepsAssignedSupplier() {
        givenRequestedOrder(requestedLine(11L, 1L, 3L), requestedLine(12L, 2L, null));
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(activeSupplier()));
        when(skuSupplierRepository.findBySkuIdIn(any())).thenReturn(List.of(mapping(1L, 3L), mapping(2L, 3L)));
        when(supplierOrderRepository.countBySupplierOrderNoPrefix(anyString())).thenReturn(0L);
        stubConfirmSaves();

        purchaseOrderService.confirmPurchaseOrder(4L, new PurchaseOrderConfirmCommand(
                5L, List.of(new PurchaseOrderConfirmCommand.Line(12L, 3L))));

        verify(supplierOrderRepository).save(any(SupplierOrder.class));
    }

    @Test
    @DisplayName("확정 처리 사용자가 없으면 VALIDATION_ERROR를 던진다")
    void confirm_userIdRequired() {
        assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, new PurchaseOrderConfirmCommand(null, null)),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, null), ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, statusHistoryUseCase);
    }

    @Test
    @DisplayName("없는 발주를 확정하면 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void confirm_notFound() {
        when(purchaseOrderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.confirmPurchaseOrder(999L, new PurchaseOrderConfirmCommand(5L, null)),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
    }

    @Test
    @DisplayName("REQUESTED가 아닌 발주를 확정하면 CONFLICT를 던지고 저장하지 않는다")
    void confirm_notRequested() {
        for (PurchaseOrderStatus status : List.of(
                PurchaseOrderStatus.CONFIRMED, PurchaseOrderStatus.COMPLETED, PurchaseOrderStatus.CANCELED,
                PurchaseOrderStatus.REJECTED)) {
            when(purchaseOrderRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(purchaseOrder(4L, status)));

            assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, new PurchaseOrderConfirmCommand(5L, null)),
                    ErrorCode.CONFLICT.name());
        }
        verify(purchaseOrderRepository, never()).save(any());
        verifyNoInteractions(supplierRepository, supplierOrderRepository);
    }

    @Test
    @DisplayName("공급처가 지정되지 않은 라인이 남으면 LINE_SUPPLIER_REQUIRED를 던지고 아무것도 만들지 않는다")
    void confirm_lineSupplierRequired() {
        givenRequestedOrder(requestedLine(11L, 1L, null), requestedLine(12L, 2L, null));

        assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, new PurchaseOrderConfirmCommand(
                        5L, List.of(new PurchaseOrderConfirmCommand.Line(11L, 3L)))),
                PurchaseOrderErrorCode.LINE_SUPPLIER_REQUIRED.name());
        verifyNoInteractions(supplierOrderRepository, statusHistoryUseCase);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("이 발주에 없는 항목이나 같은 항목의 중복 지정은 VALIDATION_ERROR를 던진다")
    void confirm_invalidLineSelection() {
        givenRequestedOrder(requestedLine(11L, 1L, null));

        assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, new PurchaseOrderConfirmCommand(
                        5L, List.of(new PurchaseOrderConfirmCommand.Line(99L, 3L)))),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, new PurchaseOrderConfirmCommand(
                        5L, List.of(new PurchaseOrderConfirmCommand.Line(11L, 3L),
                                new PurchaseOrderConfirmCommand.Line(11L, 7L)))),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(supplierOrderRepository);
    }

    @Test
    @DisplayName("없는 공급처를 지정하면 SUPPLIER_NOT_FOUND를 던진다")
    void confirm_supplierNotFound() {
        givenRequestedOrder(requestedLine(11L, 1L, null));
        when(supplierRepository.findById(3L)).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, new PurchaseOrderConfirmCommand(
                        5L, List.of(new PurchaseOrderConfirmCommand.Line(11L, 3L)))),
                SupplierErrorCode.SUPPLIER_NOT_FOUND.name());
        verifyNoInteractions(supplierOrderRepository);
    }

    @Test
    @DisplayName("공급처가 비활성이면 SUPPLIER_INACTIVE를 던지고 상태를 바꾸지 않는다")
    void confirm_supplierInactive() {
        PurchaseOrderLine line = requestedLine(11L, 1L, null);
        PurchaseOrder requested = purchaseOrder(4L, PurchaseOrderStatus.REQUESTED);
        when(purchaseOrderRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(requested));
        when(purchaseOrderRepository.findLinesByPurchaseOrderIdForUpdate(4L)).thenReturn(List.of(line));
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(inactiveSupplier()));

        assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, new PurchaseOrderConfirmCommand(
                        5L, List.of(new PurchaseOrderConfirmCommand.Line(11L, 3L)))),
                PurchaseOrderErrorCode.SUPPLIER_INACTIVE.name());
        assertThat(requested.getStatus()).isEqualTo(PurchaseOrderStatus.REQUESTED);
        assertThat(line.getSupplierId()).isNull();
        verify(purchaseOrderRepository, never()).save(any());
        verifyNoInteractions(supplierOrderRepository);
    }

    @Test
    @DisplayName("SKU에 매핑되지 않은 공급처를 지정하면 SKU_SUPPLIER_NOT_MAPPED를 던지고 아무것도 만들지 않는다")
    void confirm_skuSupplierNotMapped() {
        givenRequestedOrder(requestedLine(11L, 1L, null));
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(activeSupplier()));
        when(skuSupplierRepository.findBySkuIdIn(any())).thenReturn(List.of(mapping(1L, 7L)));

        assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, new PurchaseOrderConfirmCommand(
                        5L, List.of(new PurchaseOrderConfirmCommand.Line(11L, 3L)))),
                PurchaseOrderErrorCode.SKU_SUPPLIER_NOT_MAPPED.name());
        verifyNoInteractions(supplierOrderRepository, statusHistoryUseCase);
        verify(purchaseOrderRepository, never()).saveLines(any());
    }

    // ---------- 반려 ----------

    @Test
    @DisplayName("본사 관리자가 REQUESTED 발주를 사유와 함께 반려하면 REJECTED로 저장하고 이력에 사유를 남긴다")
    void reject_success() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.REQUESTED)));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = purchaseOrderService.rejectPurchaseOrder(
                4L, new PurchaseOrderRejectCommand("수량이 과다함", 1L), HQ);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.REJECTED);
        verify(statusHistoryUseCase).record(
                InboundHistoryEntityType.PURCHASE_ORDER.name(), 4L, "REQUESTED", "REJECTED", "수량이 과다함", 1L);
        verifyNoInteractions(supplierOrderRepository);
    }

    @Test
    @DisplayName("반려 사유가 없거나 공백이거나 500자를 넘으면 VALIDATION_ERROR를 던진다")
    void reject_reasonValidation() {
        assertError(() -> purchaseOrderService.rejectPurchaseOrder(4L, new PurchaseOrderRejectCommand(null, 1L), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> purchaseOrderService.rejectPurchaseOrder(4L, new PurchaseOrderRejectCommand("  ", 1L), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> purchaseOrderService.rejectPurchaseOrder(
                        4L, new PurchaseOrderRejectCommand("가".repeat(501), 1L), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> purchaseOrderService.rejectPurchaseOrder(4L, new PurchaseOrderRejectCommand("사유", null), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, statusHistoryUseCase);
    }

    @Test
    @DisplayName("창고 관리자는 반려할 수 없다(403)")
    void reject_warehouseManager_forbidden() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.REQUESTED)));

        assertError(() -> purchaseOrderService.rejectPurchaseOrder(
                        4L, new PurchaseOrderRejectCommand("사유", 5L), AUTHOR),
                ErrorCode.FORBIDDEN.name());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("REQUESTED가 아닌 발주를 반려하면 CONFLICT를 던지고, 없는 발주는 PURCHASE_ORDER_NOT_FOUND다")
    void reject_conflictAndNotFound() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.CONFIRMED)));
        when(purchaseOrderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.rejectPurchaseOrder(
                        4L, new PurchaseOrderRejectCommand("사유", 1L), HQ), ErrorCode.CONFLICT.name());
        assertError(() -> purchaseOrderService.rejectPurchaseOrder(
                        999L, new PurchaseOrderRejectCommand("사유", 1L), HQ),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
        verify(purchaseOrderRepository, never()).save(any());
    }

    // ---------- 취소 ----------

    @Test
    @DisplayName("REQUESTED 발주는 사유 없이도 취소할 수 있다")
    void cancel_requestedWithoutReason() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.REQUESTED)));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = purchaseOrderService.cancelPurchaseOrder(4L, new PurchaseOrderCancelCommand(null, 5L), AUTHOR);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
        verify(statusHistoryUseCase).record(
                InboundHistoryEntityType.PURCHASE_ORDER.name(), 4L, "REQUESTED", "CANCELED", null, 5L);
    }

    @Test
    @DisplayName("CONFIRMED 발주는 사유와 함께 취소할 수 있다")
    void cancel_confirmedWithReason() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.CONFIRMED)));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = purchaseOrderService.cancelPurchaseOrder(
                4L, new PurchaseOrderCancelCommand("공급업체 재고 부족으로 납품 불가", 5L), HQ);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
        verify(statusHistoryUseCase).record(InboundHistoryEntityType.PURCHASE_ORDER.name(), 4L,
                "CONFIRMED", "CANCELED", "공급업체 재고 부족으로 납품 불가", 5L);
    }

    @Test
    @DisplayName("CONFIRMED 발주를 취소하면 진행 중인 공급처 발주서도 함께 CANCELED로 바꾸고 이력을 남긴다")
    void cancel_confirmed_cancelsSupplierOrders() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.CONFIRMED)));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(supplierOrderRepository.findByPurchaseOrderIdForUpdate(4L)).thenReturn(List.of(
                SupplierOrder.builder().supplierOrderId(21L).purchaseOrderId(4L).supplierId(3L)
                        .supplierOrderNo("SO-1").status(SupplierOrderStatus.ISSUED).build(),
                SupplierOrder.builder().supplierOrderId(22L).purchaseOrderId(4L).supplierId(7L)
                        .supplierOrderNo("SO-2").status(SupplierOrderStatus.PARTIALLY_RECEIVED).build()));
        when(supplierOrderRepository.save(any(SupplierOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        purchaseOrderService.cancelPurchaseOrder(4L, new PurchaseOrderCancelCommand("공급 불가", 1L), HQ);

        ArgumentCaptor<SupplierOrder> saved = ArgumentCaptor.forClass(SupplierOrder.class);
        verify(supplierOrderRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(SupplierOrder::getStatus)
                .containsOnly(SupplierOrderStatus.CANCELED);
        verify(statusHistoryUseCase).record(InboundHistoryEntityType.SUPPLIER_ORDER.name(), 21L,
                "ISSUED", "CANCELED", "공급 불가", 1L);
        verify(statusHistoryUseCase).record(InboundHistoryEntityType.SUPPLIER_ORDER.name(), 22L,
                "PARTIALLY_RECEIVED", "CANCELED", "공급 불가", 1L);
    }

    @Test
    @DisplayName("취소 처리 사용자가 없으면 VALIDATION_ERROR를 던진다")
    void cancel_userIdRequired() {
        assertError(() -> purchaseOrderService.cancelPurchaseOrder(4L, new PurchaseOrderCancelCommand("사유", null), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> purchaseOrderService.cancelPurchaseOrder(4L, null, HQ), ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, statusHistoryUseCase);
    }

    @Test
    @DisplayName("CONFIRMED 발주를 사유 없이(null·공백) 취소하면 VALIDATION_ERROR를 던진다")
    void cancel_confirmedWithoutReason() {
        PurchaseOrder confirmed = purchaseOrder(4L, PurchaseOrderStatus.CONFIRMED);
        when(purchaseOrderRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(confirmed));

        assertError(() -> purchaseOrderService.cancelPurchaseOrder(4L, new PurchaseOrderCancelCommand(null, 5L), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> purchaseOrderService.cancelPurchaseOrder(4L, new PurchaseOrderCancelCommand("  ", 5L), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> purchaseOrderService.cancelPurchaseOrder(4L, null, HQ),
                ErrorCode.VALIDATION_ERROR.name());
        assertThat(confirmed.getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        verify(purchaseOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("취소 사유가 500자를 넘으면 VALIDATION_ERROR를 던진다")
    void cancel_reasonTooLong() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.REQUESTED)));

        assertError(() -> purchaseOrderService.cancelPurchaseOrder(
                        4L, new PurchaseOrderCancelCommand("가".repeat(501), 5L), AUTHOR),
                ErrorCode.VALIDATION_ERROR.name());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 취소·완료된 발주를 취소하면 CONFLICT를 던진다")
    void cancel_alreadyFinished() {
        for (PurchaseOrderStatus status : List.of(PurchaseOrderStatus.CANCELED, PurchaseOrderStatus.COMPLETED)) {
            when(purchaseOrderRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(purchaseOrder(4L, status)));

            assertError(() -> purchaseOrderService.cancelPurchaseOrder(
                            4L, new PurchaseOrderCancelCommand("사유", 5L), HQ),
                    ErrorCode.CONFLICT.name());
        }
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("취소되지 않은 입고가 있는 발주는 PURCHASE_ORDER_HAS_INBOUND로 취소할 수 없다")
    void cancel_hasInbound() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.CONFIRMED)));
        when(inboundRepository.existsNotCanceledByPurchaseOrderId(4L)).thenReturn(true);

        assertError(() -> purchaseOrderService.cancelPurchaseOrder(4L, new PurchaseOrderCancelCommand("입고 전 취소", 5L), HQ),
                PurchaseOrderErrorCode.PURCHASE_ORDER_HAS_INBOUND.name());
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("없는 발주를 취소하면 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void cancel_notFound() {
        when(purchaseOrderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.cancelPurchaseOrder(999L, new PurchaseOrderCancelCommand("사유", 5L), HQ),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
    }

    // ---------- 조회 ----------

    @Test
    @DisplayName("목록 조회는 검색 조건을 그대로 조회 포트에 위임한다")
    void getPurchaseOrders_delegates() {
        PurchaseOrderSearchCondition condition = PurchaseOrderSearchCondition.unscoped(
                PurchaseOrderStatus.REQUESTED, 1L, 3L, "PO", null, null);
        when(supplierRepository.existsById(3L)).thenReturn(true);
        when(purchaseOrderQueryRepository.search(condition)).thenReturn(List.of());

        assertThat(purchaseOrderService.getPurchaseOrders(condition, HQ)).isEmpty();
        verify(warehouseAvailabilityPort).requireExists(1L);
    }

    @Test
    @DisplayName("목록 항목에는 라인에 배정된 공급처를 한 번의 조회로 채운다(배정 전에는 빈 목록)")
    void getPurchaseOrders_fillsSuppliers() {
        PurchaseOrderSearchCondition condition =
                PurchaseOrderSearchCondition.unscoped(null, null, null, null, null, null);
        when(purchaseOrderQueryRepository.search(condition)).thenReturn(List.of(
                new PurchaseOrderSummary(4L, "PO-1", 1L, "서울 물류센터", PurchaseOrderStatus.CONFIRMED, null, 2L,
                        BigDecimal.TEN, 5L, null, null),
                new PurchaseOrderSummary(5L, "PO-2", 1L, "서울 물류센터", PurchaseOrderStatus.REQUESTED, null, 1L,
                        BigDecimal.ONE, 5L, null, null)));
        when(purchaseOrderQueryRepository.findSuppliers(List.of(4L, 5L))).thenReturn(List.of(
                new PurchaseOrderSupplierView(4L, 3L, "공급처 A"), new PurchaseOrderSupplierView(4L, 7L, "공급처 B")));

        List<PurchaseOrderSummary> result = purchaseOrderService.getPurchaseOrders(condition, HQ);

        assertThat(result.get(0).suppliers()).extracting(PurchaseOrderSupplierView::supplierId)
                .containsExactly(3L, 7L);
        assertThat(result.get(1).suppliers()).isEmpty();
    }

    @Test
    @DisplayName("등록 시작 일시가 종료 일시보다 늦으면 VALIDATION_ERROR를 던진다")
    void getPurchaseOrders_invalidDateRange() {
        PurchaseOrderSearchCondition condition = PurchaseOrderSearchCondition.unscoped(
                null, null, null, null,
                LocalDateTime.of(2026, 10, 2, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0));

        assertError(() -> purchaseOrderService.getPurchaseOrders(condition, HQ), ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderQueryRepository);
    }

    @Test
    @DisplayName("없는 발주를 조회하면 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void getPurchaseOrder_notFound() {
        when(purchaseOrderQueryRepository.findView(999L)).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.getPurchaseOrder(999L, HQ),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
    }

    @Test
    @DisplayName("취소된 발주를 조회하면 상태 이력의 취소 사유를 cancelReason으로 채운다")
    void getPurchaseOrder_canceled_fillsCancelReason() {
        when(purchaseOrderQueryRepository.findView(4L))
                .thenReturn(Optional.of(viewWithStatus(PurchaseOrderStatus.CANCELED)));
        when(statusHistoryUseCase.findStatusReason(InboundHistoryEntityType.PURCHASE_ORDER.name(), 4L, "CANCELED"))
                .thenReturn(Optional.of("공급업체 재고 부족"));

        assertThat(purchaseOrderService.getPurchaseOrder(4L, HQ).cancelReason()).isEqualTo("공급업체 재고 부족");
    }

    @Test
    @DisplayName("사유 없이 취소한 발주는 cancelReason이 null이다")
    void getPurchaseOrder_canceledWithoutReason() {
        when(purchaseOrderQueryRepository.findView(4L))
                .thenReturn(Optional.of(viewWithStatus(PurchaseOrderStatus.CANCELED)));
        when(statusHistoryUseCase.findStatusReason(InboundHistoryEntityType.PURCHASE_ORDER.name(), 4L, "CANCELED"))
                .thenReturn(Optional.empty());

        assertThat(purchaseOrderService.getPurchaseOrder(4L, HQ).cancelReason()).isNull();
    }

    @Test
    @DisplayName("취소·반려 상태가 아닌 발주는 상태 이력을 조회하지 않고 사유가 null이며 공급처 발주서를 담는다")
    void getPurchaseOrder_notCanceled_noReasons() {
        SupplierOrderSummary supplierOrder = new SupplierOrderSummary(
                21L, "SO-20261002-0001", 4L, "PO-20261002-0001", 3L, "공급처 A", 1L, "서울 물류센터",
                SupplierOrderStatus.ISSUED, 1L, BigDecimal.TEN, null);
        when(purchaseOrderQueryRepository.findView(4L))
                .thenReturn(Optional.of(viewWithStatus(PurchaseOrderStatus.CONFIRMED)));
        when(supplierOrderQueryRepository.findSummariesByPurchaseOrderId(4L)).thenReturn(List.of(supplierOrder));

        PurchaseOrderView result = purchaseOrderService.getPurchaseOrder(4L, HQ);

        assertThat(result.cancelReason()).isNull();
        assertThat(result.rejectReason()).isNull();
        assertThat(result.supplierOrders()).containsExactly(supplierOrder);
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("반려된 발주를 조회하면 상태 이력의 반려 사유를 rejectReason으로 채운다")
    void getPurchaseOrder_rejected_fillsRejectReason() {
        when(purchaseOrderQueryRepository.findView(4L))
                .thenReturn(Optional.of(viewWithStatus(PurchaseOrderStatus.REJECTED)));
        when(statusHistoryUseCase.findStatusReason(InboundHistoryEntityType.PURCHASE_ORDER.name(), 4L, "REJECTED"))
                .thenReturn(Optional.of("수량이 과다함"));

        PurchaseOrderView result = purchaseOrderService.getPurchaseOrder(4L, HQ);

        assertThat(result.rejectReason()).isEqualTo("수량이 과다함");
        assertThat(result.cancelReason()).isNull();
    }

    @Test
    @DisplayName("발주 상세는 헤더 식별 정보와 SKU별 항목을 합쳐 반환한다")
    void getPurchaseOrderDetails_success() {
        PurchaseOrderView view = new PurchaseOrderView(
                4L, "PO-20261002-0001", 1L, "서울 물류센터", PurchaseOrderStatus.CONFIRMED,
                null, null, 1L, BigDecimal.valueOf(6000000), 5L, null, null, null);
        PurchaseOrderLineView lineView = new PurchaseOrderLineView(
                11L, 1L, "SKU-0001", "배드민턴 라켓", "EA", 100L, 60L, 40L,
                BigDecimal.valueOf(60000), BigDecimal.valueOf(6000000), PurchaseOrderLineStatus.PARTIALLY_RECEIVED,
                3L, "공급처 A", 21L);
        when(purchaseOrderQueryRepository.findView(4L)).thenReturn(Optional.of(view));
        when(purchaseOrderQueryRepository.findLineViews(4L)).thenReturn(List.of(lineView));

        PurchaseOrderDetails details = purchaseOrderService.getPurchaseOrderDetails(4L, HQ);

        assertThat(details.purchaseOrderId()).isEqualTo(4L);
        assertThat(details.purchaseOrderNo()).isEqualTo("PO-20261002-0001");
        assertThat(details.status()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        assertThat(details.items()).containsExactly(lineView);
    }

    @Test
    @DisplayName("없는 발주의 상세를 조회하면 항목을 조회하지 않고 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void getPurchaseOrderDetails_notFound() {
        when(purchaseOrderQueryRepository.findView(anyLong())).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.getPurchaseOrderDetails(999L, HQ),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
        verify(purchaseOrderQueryRepository, never()).findLineViews(anyLong());
    }

    @Test
    @DisplayName("요청 발주는 작성자인 창고 관리자만 취소할 수 있고, 다른 창고 관리자와 본사는 403이다")
    void cancel_requested_onlyAuthorWarehouseManager() {
        AuthenticatedUser otherManager = new AuthenticatedUser(6L, UserRole.WAREHOUSE_MANAGER, List.of(1L), List.of());
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.REQUESTED)));

        for (AuthenticatedUser actor : List.of(otherManager, HQ)) {
            assertError(() -> purchaseOrderService.cancelPurchaseOrder(
                    4L, new PurchaseOrderCancelCommand(null, actor.userId()), actor), ErrorCode.FORBIDDEN.name());
        }
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("확정 발주는 본사 관리자만 취소할 수 있고, 작성자 창고 관리자도 403이다")
    void cancel_confirmed_onlyHqAdmin() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.CONFIRMED)));

        assertError(() -> purchaseOrderService.cancelPurchaseOrder(
                4L, new PurchaseOrderCancelCommand("사유", 5L), AUTHOR), ErrorCode.FORBIDDEN.name());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("발주 등록은 담당 창고에만 할 수 있다")
    void register_otherWarehouse_forbidden() {
        AuthenticatedUser manager = new AuthenticatedUser(5L, UserRole.WAREHOUSE_MANAGER, List.of(9L), List.of());
        PurchaseOrderRegisterCommand command = new PurchaseOrderRegisterCommand(
                1L, null, null, 5L, List.of(new PurchaseOrderRegisterCommand.Line(3L, 10L)));

        assertError(() -> purchaseOrderService.registerPurchaseOrder(command, manager), ErrorCode.FORBIDDEN.name());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("발주 목록은 창고를 생략하면 담당 창고로 좁히고, 비담당 창고를 지정하면 403이다. 단건은 비담당 창고면 403이다")
    void scope_listAndDetail() {
        AuthenticatedUser manager = new AuthenticatedUser(5L, UserRole.WAREHOUSE_MANAGER, List.of(1L, 2L), List.of());
        PurchaseOrderSearchCondition all = PurchaseOrderSearchCondition.unscoped(null, null, null, null, null, null);
        when(purchaseOrderQueryRepository.search(
                new PurchaseOrderSearchCondition(null, null, null, null, null, null, List.of(1L, 2L))))
                .thenReturn(List.of());

        assertThat(purchaseOrderService.getPurchaseOrders(all, manager)).isEmpty();
        assertError(() -> purchaseOrderService.getPurchaseOrders(
                PurchaseOrderSearchCondition.unscoped(null, 3L, null, null, null, null), manager), ErrorCode.FORBIDDEN.name());
    }


    @Test
    @DisplayName("다른 창고 사용자의 취소는 발주 상태와 상관없이 403이다 (상태 409가 드러나지 않는다). 소속이 바뀐 작성자도 마찬가지다")
    void cancel_otherWarehouse_forbiddenBeforeStateCheck() {
        // 작성자(5번)의 소속이 창고 9로 바뀌어 발주 창고(1)의 담당이 아닌 경우
        AuthenticatedUser movedAuthor = new AuthenticatedUser(5L, UserRole.WAREHOUSE_MANAGER, List.of(9L), List.of());
        AuthenticatedUser otherManager = new AuthenticatedUser(6L, UserRole.WAREHOUSE_MANAGER, List.of(9L), List.of());

        for (PurchaseOrderStatus status : PurchaseOrderStatus.values()) {
            when(purchaseOrderRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(purchaseOrder(4L, status)));
            for (AuthenticatedUser actor : List.of(movedAuthor, otherManager)) {
                assertError(() -> purchaseOrderService.cancelPurchaseOrder(
                        4L, new PurchaseOrderCancelCommand("사유", actor.userId()), actor), ErrorCode.FORBIDDEN.name());
            }
        }
        verify(purchaseOrderRepository, never()).save(any());
        // 담당 창고의 사용자에게는 종료된 발주가 그대로 409다
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.COMPLETED)));
        assertError(() -> purchaseOrderService.cancelPurchaseOrder(
                4L, new PurchaseOrderCancelCommand("사유", 1L), HQ), ErrorCode.CONFLICT.name());
    }
}

package com.kb.wms.inbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderCancelCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRegisterCommand;
import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderDetails;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderQueryRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SkuPurchasePricePort;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.application.port.out.WarehouseAvailabilityPort;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inbound.exception.SupplierErrorCode;

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
                .supplierId(3L)
                .status(status)
                .createdBy(5L)
                .build();
    }

    private static PurchaseOrderRegisterCommand registerCommand(LocalDateTime expectedAt, String note,
                                                                PurchaseOrderRegisterCommand.Line... lines) {
        return new PurchaseOrderRegisterCommand(1L, 3L, expectedAt, note, 5L, List.of(lines));
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

    private static PurchaseOrderView viewWithStatus(PurchaseOrderStatus status) {
        return new PurchaseOrderView(
                4L, "PO-20261002-0001", 1L, "서울 물류센터", 3L, "공급처 A", status,
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
                    .supplierId(source.getSupplierId())
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
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(activeSupplier()));
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
        assertThat(header.getSupplierId()).isEqualTo(3L);
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
                StatusHistoryEntityType.PURCHASE_ORDER, 4L, null, "REQUESTED", null, 5L);
    }

    @Test
    @DisplayName("입고 예정 일시와 비고 없이도 등록할 수 있고 당일 첫 발주 번호는 0001이다")
    void register_withoutOptionalFields() {
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(activeSupplier()));
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
    @DisplayName("공급처가 없으면 SUPPLIER_NOT_FOUND를 던진다")
    void register_supplierNotFound() {
        when(supplierRepository.findById(3L)).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 10)), HQ),
                SupplierErrorCode.SUPPLIER_NOT_FOUND.name());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("공급처가 비활성이면 CONFLICT를 던진다")
    void register_supplierInactive() {
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(inactiveSupplier()));

        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 10)), HQ),
                ErrorCode.CONFLICT.name());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("SKU에 매입 단가가 없으면 PURCHASE_PRICE_MISSING이 전달되고 저장하지 않는다")
    void register_purchasePriceMissing() {
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(activeSupplier()));
        when(skuPurchasePricePort.getPurchasablePrice(1L))
                .thenThrow(new BusinessException(PurchaseOrderErrorCode.PURCHASE_PRICE_MISSING));

        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 10)), HQ),
                PurchaseOrderErrorCode.PURCHASE_PRICE_MISSING.name());
        verify(purchaseOrderRepository, never()).save(any());
        verify(purchaseOrderRepository, never()).saveLines(any());
    }

    // ---------- 확정 ----------

    @Test
    @DisplayName("REQUESTED 발주를 확정하면 CONFIRMED로 저장한다")
    void confirm_success() {
        when(purchaseOrderRepository.findById(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.REQUESTED)));
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(activeSupplier()));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = purchaseOrderService.confirmPurchaseOrder(4L, 5L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        verify(purchaseOrderRepository).save(result);
        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.PURCHASE_ORDER, 4L, "REQUESTED", "CONFIRMED", null, 5L);
    }

    @Test
    @DisplayName("확정 처리 사용자가 없으면 VALIDATION_ERROR를 던진다")
    void confirm_userIdRequired() {
        assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, null), ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, statusHistoryUseCase);
    }

    @Test
    @DisplayName("없는 발주를 확정하면 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void confirm_notFound() {
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.confirmPurchaseOrder(999L, 5L),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
    }

    @Test
    @DisplayName("REQUESTED가 아닌 발주를 확정하면 CONFLICT를 던지고 저장하지 않는다")
    void confirm_notRequested() {
        for (PurchaseOrderStatus status : List.of(
                PurchaseOrderStatus.CONFIRMED, PurchaseOrderStatus.COMPLETED, PurchaseOrderStatus.CANCELED)) {
            when(purchaseOrderRepository.findById(4L)).thenReturn(Optional.of(purchaseOrder(4L, status)));

            assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, 5L), ErrorCode.CONFLICT.name());
        }
        verify(purchaseOrderRepository, never()).save(any());
        verifyNoInteractions(supplierRepository);
    }

    @Test
    @DisplayName("공급처가 비활성이면 SUPPLIER_INACTIVE를 던지고 상태를 바꾸지 않는다")
    void confirm_supplierInactive() {
        PurchaseOrder requested = purchaseOrder(4L, PurchaseOrderStatus.REQUESTED);
        when(purchaseOrderRepository.findById(4L)).thenReturn(Optional.of(requested));
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(inactiveSupplier()));

        assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L, 5L),
                PurchaseOrderErrorCode.SUPPLIER_INACTIVE.name());
        assertThat(requested.getStatus()).isEqualTo(PurchaseOrderStatus.REQUESTED);
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
                StatusHistoryEntityType.PURCHASE_ORDER, 4L, "REQUESTED", "CANCELED", null, 5L);
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
        verify(statusHistoryUseCase).record(StatusHistoryEntityType.PURCHASE_ORDER, 4L,
                "CONFIRMED", "CANCELED", "공급업체 재고 부족으로 납품 불가", 5L);
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
        List<PurchaseOrderSummary> expected = List.of();
        when(purchaseOrderQueryRepository.search(condition)).thenReturn(expected);

        assertThat(purchaseOrderService.getPurchaseOrders(condition, HQ)).isSameAs(expected);
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
        when(statusHistoryUseCase.findStatusReason(StatusHistoryEntityType.PURCHASE_ORDER, 4L, "CANCELED"))
                .thenReturn(Optional.of("공급업체 재고 부족"));

        assertThat(purchaseOrderService.getPurchaseOrder(4L, HQ).cancelReason()).isEqualTo("공급업체 재고 부족");
    }

    @Test
    @DisplayName("사유 없이 취소한 발주는 cancelReason이 null이다")
    void getPurchaseOrder_canceledWithoutReason() {
        when(purchaseOrderQueryRepository.findView(4L))
                .thenReturn(Optional.of(viewWithStatus(PurchaseOrderStatus.CANCELED)));
        when(statusHistoryUseCase.findStatusReason(StatusHistoryEntityType.PURCHASE_ORDER, 4L, "CANCELED"))
                .thenReturn(Optional.empty());

        assertThat(purchaseOrderService.getPurchaseOrder(4L, HQ).cancelReason()).isNull();
    }

    @Test
    @DisplayName("취소 상태가 아닌 발주는 상태 이력을 조회하지 않고 cancelReason이 null이다")
    void getPurchaseOrder_notCanceled_noCancelReason() {
        when(purchaseOrderQueryRepository.findView(4L))
                .thenReturn(Optional.of(viewWithStatus(PurchaseOrderStatus.CONFIRMED)));

        assertThat(purchaseOrderService.getPurchaseOrder(4L, HQ).cancelReason()).isNull();
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("발주 상세는 헤더 식별 정보와 SKU별 항목을 합쳐 반환한다")
    void getPurchaseOrderDetails_success() {
        PurchaseOrderView view = new PurchaseOrderView(
                4L, "PO-20261002-0001", 1L, "서울 물류센터", 3L, "공급처 A", PurchaseOrderStatus.CONFIRMED,
                null, null, 1L, BigDecimal.valueOf(6000000), 5L, null, null, null);
        PurchaseOrderLineView lineView = new PurchaseOrderLineView(
                11L, 1L, "SKU-0001", "배드민턴 라켓", "EA", 100L, 60L, 40L,
                BigDecimal.valueOf(60000), BigDecimal.valueOf(6000000), PurchaseOrderLineStatus.PARTIALLY_RECEIVED);
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
                1L, 2L, null, null, 5L, List.of(new PurchaseOrderRegisterCommand.Line(3L, 10L)));

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

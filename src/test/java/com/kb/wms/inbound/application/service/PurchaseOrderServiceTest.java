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

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderCancelCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRegisterCommand;
import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderDetails;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
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
                registerCommand(expectedAt, "정기 보충 발주", line(1L, 100), line(2L, 10)));

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
    }

    @Test
    @DisplayName("입고 예정 일시와 비고 없이도 등록할 수 있고 당일 첫 발주 번호는 0001이다")
    void register_withoutOptionalFields() {
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(activeSupplier()));
        when(skuPurchasePricePort.getPurchasablePrice(1L)).thenReturn(BigDecimal.valueOf(60000));
        when(purchaseOrderRepository.countByPurchaseOrderNoPrefix(anyString())).thenReturn(0L);
        stubSaveAssigningId(4L);

        purchaseOrderService.registerPurchaseOrder(registerCommand(null, null, line(1L, 1)));

        ArgumentCaptor<PurchaseOrder> headerCaptor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepository).save(headerCaptor.capture());
        assertThat(headerCaptor.getValue().getPurchaseOrderNo()).matches("PO-\\d{8}-0001");
        assertThat(headerCaptor.getValue().getExpectedAt()).isNull();
        assertThat(headerCaptor.getValue().getNote()).isNull();
    }

    @Test
    @DisplayName("발주 항목이 비어 있으면 VALIDATION_ERROR를 던진다")
    void register_emptyLines() {
        assertError(() -> purchaseOrderService.registerPurchaseOrder(registerCommand(null, null)),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, warehouseAvailabilityPort, skuPurchasePricePort);
    }

    @Test
    @DisplayName("발주 수량이 0 이하면 VALIDATION_ERROR를 던진다")
    void register_nonPositiveQuantity() {
        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 0))),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, warehouseAvailabilityPort, skuPurchasePricePort);
    }

    @Test
    @DisplayName("같은 SKU를 두 번 넣으면 VALIDATION_ERROR를 던진다")
    void register_duplicateSku() {
        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 10), line(1L, 20))),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, warehouseAvailabilityPort, skuPurchasePricePort);
    }

    @Test
    @DisplayName("입고 예정 일시가 과거면 VALIDATION_ERROR를 던진다")
    void register_expectedAtInPast() {
        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(LocalDateTime.now().minusMinutes(1), null, line(1L, 10))),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, warehouseAvailabilityPort, skuPurchasePricePort);
    }

    @Test
    @DisplayName("비고가 1000자를 넘으면 VALIDATION_ERROR를 던진다")
    void register_noteTooLong() {
        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, "가".repeat(1001), line(1L, 10))),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, warehouseAvailabilityPort, skuPurchasePricePort);
    }

    @Test
    @DisplayName("창고가 비활성이면 창고 포트의 예외가 그대로 전달되고 저장하지 않는다")
    void register_warehouseInactive() {
        doThrow(new BusinessException(ErrorCode.CONFLICT, "비활성 창고에는 발주를 등록할 수 없습니다."))
                .when(warehouseAvailabilityPort).requireActive(1L);

        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 10))),
                ErrorCode.CONFLICT.name());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("공급처가 없으면 SUPPLIER_NOT_FOUND를 던진다")
    void register_supplierNotFound() {
        when(supplierRepository.findById(3L)).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 10))),
                SupplierErrorCode.SUPPLIER_NOT_FOUND.name());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("공급처가 비활성이면 CONFLICT를 던진다")
    void register_supplierInactive() {
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(inactiveSupplier()));

        assertError(() -> purchaseOrderService.registerPurchaseOrder(
                        registerCommand(null, null, line(1L, 10))),
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
                        registerCommand(null, null, line(1L, 10))),
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

        PurchaseOrder result = purchaseOrderService.confirmPurchaseOrder(4L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        verify(purchaseOrderRepository).save(result);
    }

    @Test
    @DisplayName("없는 발주를 확정하면 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void confirm_notFound() {
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.confirmPurchaseOrder(999L),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
    }

    @Test
    @DisplayName("REQUESTED가 아닌 발주를 확정하면 CONFLICT를 던지고 저장하지 않는다")
    void confirm_notRequested() {
        for (PurchaseOrderStatus status : List.of(
                PurchaseOrderStatus.CONFIRMED, PurchaseOrderStatus.COMPLETED, PurchaseOrderStatus.CANCELED)) {
            when(purchaseOrderRepository.findById(4L)).thenReturn(Optional.of(purchaseOrder(4L, status)));

            assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L), ErrorCode.CONFLICT.name());
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

        assertError(() -> purchaseOrderService.confirmPurchaseOrder(4L),
                PurchaseOrderErrorCode.SUPPLIER_INACTIVE.name());
        assertThat(requested.getStatus()).isEqualTo(PurchaseOrderStatus.REQUESTED);
        verify(purchaseOrderRepository, never()).save(any());
    }

    // ---------- 취소 ----------

    @Test
    @DisplayName("REQUESTED 발주는 사유 없이도 취소할 수 있다")
    void cancel_requestedWithoutReason() {
        when(purchaseOrderRepository.findById(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.REQUESTED)));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = purchaseOrderService.cancelPurchaseOrder(4L, new PurchaseOrderCancelCommand(null));

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
    }

    @Test
    @DisplayName("취소 요청 본문이 없어도 REQUESTED 발주는 취소할 수 있다")
    void cancel_requestedWithoutCommand() {
        when(purchaseOrderRepository.findById(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.REQUESTED)));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = purchaseOrderService.cancelPurchaseOrder(4L, null);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
    }

    @Test
    @DisplayName("CONFIRMED 발주는 사유와 함께 취소할 수 있다")
    void cancel_confirmedWithReason() {
        when(purchaseOrderRepository.findById(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.CONFIRMED)));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = purchaseOrderService.cancelPurchaseOrder(
                4L, new PurchaseOrderCancelCommand("공급업체 재고 부족으로 납품 불가"));

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
    }

    @Test
    @DisplayName("CONFIRMED 발주를 사유 없이(null·공백) 취소하면 VALIDATION_ERROR를 던진다")
    void cancel_confirmedWithoutReason() {
        PurchaseOrder confirmed = purchaseOrder(4L, PurchaseOrderStatus.CONFIRMED);
        when(purchaseOrderRepository.findById(4L)).thenReturn(Optional.of(confirmed));

        assertError(() -> purchaseOrderService.cancelPurchaseOrder(4L, new PurchaseOrderCancelCommand(null)),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> purchaseOrderService.cancelPurchaseOrder(4L, new PurchaseOrderCancelCommand("  ")),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> purchaseOrderService.cancelPurchaseOrder(4L, null),
                ErrorCode.VALIDATION_ERROR.name());
        assertThat(confirmed.getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("취소 사유가 500자를 넘으면 VALIDATION_ERROR를 던진다")
    void cancel_reasonTooLong() {
        when(purchaseOrderRepository.findById(4L))
                .thenReturn(Optional.of(purchaseOrder(4L, PurchaseOrderStatus.REQUESTED)));

        assertError(() -> purchaseOrderService.cancelPurchaseOrder(
                        4L, new PurchaseOrderCancelCommand("가".repeat(501))),
                ErrorCode.VALIDATION_ERROR.name());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 취소·완료된 발주를 취소하면 CONFLICT를 던진다")
    void cancel_alreadyFinished() {
        for (PurchaseOrderStatus status : List.of(PurchaseOrderStatus.CANCELED, PurchaseOrderStatus.COMPLETED)) {
            when(purchaseOrderRepository.findById(4L)).thenReturn(Optional.of(purchaseOrder(4L, status)));

            assertError(() -> purchaseOrderService.cancelPurchaseOrder(
                            4L, new PurchaseOrderCancelCommand("사유")),
                    ErrorCode.CONFLICT.name());
        }
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 발주를 취소하면 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void cancel_notFound() {
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.cancelPurchaseOrder(999L, new PurchaseOrderCancelCommand("사유")),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
    }

    // ---------- 조회 ----------

    @Test
    @DisplayName("목록 조회는 검색 조건을 그대로 조회 포트에 위임한다")
    void getPurchaseOrders_delegates() {
        PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition(
                PurchaseOrderStatus.REQUESTED, 1L, 3L, "PO", null, null);
        List<PurchaseOrderSummary> expected = List.of();
        when(purchaseOrderQueryRepository.search(condition)).thenReturn(expected);

        assertThat(purchaseOrderService.getPurchaseOrders(condition)).isSameAs(expected);
    }

    @Test
    @DisplayName("등록 시작 일시가 종료 일시보다 늦으면 VALIDATION_ERROR를 던진다")
    void getPurchaseOrders_invalidDateRange() {
        PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition(
                null, null, null, null,
                LocalDateTime.of(2026, 10, 2, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0));

        assertError(() -> purchaseOrderService.getPurchaseOrders(condition), ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderQueryRepository);
    }

    @Test
    @DisplayName("없는 발주를 조회하면 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void getPurchaseOrder_notFound() {
        when(purchaseOrderQueryRepository.findView(999L)).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.getPurchaseOrder(999L),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
    }

    @Test
    @DisplayName("발주 상세는 헤더 식별 정보와 SKU별 항목을 합쳐 반환한다")
    void getPurchaseOrderDetails_success() {
        PurchaseOrderView view = new PurchaseOrderView(
                4L, "PO-20261002-0001", 1L, "서울 물류센터", 3L, "공급처 A", PurchaseOrderStatus.CONFIRMED,
                null, null, 1L, BigDecimal.valueOf(6000000), 5L, null, null);
        PurchaseOrderLineView lineView = new PurchaseOrderLineView(
                11L, 1L, "SKU-0001", "배드민턴 라켓", "EA", 100L, 60L, 40L,
                BigDecimal.valueOf(60000), BigDecimal.valueOf(6000000), PurchaseOrderLineStatus.PARTIALLY_RECEIVED);
        when(purchaseOrderQueryRepository.findView(4L)).thenReturn(Optional.of(view));
        when(purchaseOrderQueryRepository.findLineViews(4L)).thenReturn(List.of(lineView));

        PurchaseOrderDetails details = purchaseOrderService.getPurchaseOrderDetails(4L);

        assertThat(details.purchaseOrderId()).isEqualTo(4L);
        assertThat(details.purchaseOrderNo()).isEqualTo("PO-20261002-0001");
        assertThat(details.status()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        assertThat(details.items()).containsExactly(lineView);
    }

    @Test
    @DisplayName("없는 발주의 상세를 조회하면 항목을 조회하지 않고 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void getPurchaseOrderDetails_notFound() {
        when(purchaseOrderQueryRepository.findView(anyLong())).thenReturn(Optional.empty());

        assertError(() -> purchaseOrderService.getPurchaseOrderDetails(999L),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
        verify(purchaseOrderQueryRepository, never()).findLineViews(anyLong());
    }
}

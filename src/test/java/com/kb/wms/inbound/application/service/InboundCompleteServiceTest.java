package com.kb.wms.inbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.inbound.application.port.in.result.InboundCompleteResult;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.InboundStockPort;
import com.kb.wms.inbound.application.port.out.InboundStockPort.ReceivedStock;
import com.kb.wms.inbound.application.port.out.InboundStockPort.StockReceipt;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.InboundLine;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.exception.InboundErrorCode;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inventory.exception.InventoryErrorCode;

@ExtendWith(MockitoExtension.class)
class InboundCompleteServiceTest {

    private static final long USER = 5L;

    @Mock
    private InboundRepository inboundRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private InboundStockPort inboundStockPort;

    @Mock
    private StatusHistoryUseCase statusHistoryUseCase;

    @InjectMocks
    private InboundCompleteService inboundCompleteService;

    // ---------- fixtures ----------

    private static Inbound inbound(InboundStatus status) {
        return Inbound.builder()
                .inboundId(7L)
                .inboundNo("IB-20261002-0001")
                .purchaseOrderId(4L)
                .warehouseId(1L)
                .status(status)
                .build();
    }

    private static PurchaseOrder purchaseOrder(PurchaseOrderStatus status) {
        return PurchaseOrder.builder()
                .purchaseOrderId(4L)
                .purchaseOrderNo("PO-20261002-0001")
                .warehouseId(1L)
                .supplierId(3L)
                .status(status)
                .build();
    }

    private static PurchaseOrderLine purchaseOrderLine(Long id, long expected, long received,
                                                       PurchaseOrderLineStatus status) {
        return PurchaseOrderLine.builder()
                .purchaseOrderLineId(id)
                .purchaseOrderId(4L)
                .skuId(id)
                .expectedQuantity(expected)
                .receivedQuantity(received)
                .orderedUnitPrice(BigDecimal.valueOf(60000))
                .lineAmount(BigDecimal.valueOf(60000).multiply(BigDecimal.valueOf(expected)))
                .status(status)
                .build();
    }

    private static InboundLine inboundLine(Long id, Long purchaseOrderLineId, Long lotId, long accepted,
                                           long defective, Long acceptedSectionId, Long defectSectionId) {
        return InboundLine.builder()
                .inboundLineId(id)
                .inboundId(7L)
                .purchaseOrderLineId(purchaseOrderLineId)
                .lotId(lotId)
                .acceptedSectionId(acceptedSectionId)
                .defectSectionId(defectSectionId)
                .receivedQuantity(accepted + defective)
                .acceptedQuantity(accepted)
                .defectiveQuantity(defective)
                .receivedUnitPrice(BigDecimal.valueOf(60000))
                .build();
    }

    private static void assertError(ThrowingCallable call, String errorCodeName) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(errorCodeName);
    }

    private void givenInspectingInbound(InboundLine... lines) {
        when(inboundRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(inbound(InboundStatus.INSPECTING)));
        when(inboundRepository.findLinesByInboundId(7L)).thenReturn(List.of(lines));
    }

    private void givenPurchaseOrder(PurchaseOrderStatus status, PurchaseOrderLine... lines) {
        when(purchaseOrderRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(purchaseOrder(status)));
        when(purchaseOrderRepository.findLinesByPurchaseOrderIdForUpdate(4L)).thenReturn(List.of(lines));
    }

    private void stubSaves() {
        when(inboundRepository.save(any(Inbound.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrderRepository.saveLines(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------- 성공 ----------

    @Test
    @DisplayName("부분 입고 완료: 합격·불량을 각 구역 재고로 반영하고 발주 항목은 PARTIALLY_RECEIVED, 발주는 CONFIRMED를 유지한다")
    void complete_partial() {
        givenInspectingInbound(inboundLine(21L, 11L, 31L, 58, 2, 2L, 9L));
        givenPurchaseOrder(PurchaseOrderStatus.CONFIRMED,
                purchaseOrderLine(11L, 100, 0, PurchaseOrderLineStatus.REQUESTED));
        when(inboundStockPort.receive(anyLong(), anyLong(), anyList())).thenReturn(List.of(
                new ReceivedStock(2L, 31L, false, 101L), new ReceivedStock(9L, 31L, true, 105L)));
        stubSaves();

        InboundCompleteResult result = inboundCompleteService.completeInbound(7L, USER);

        ArgumentCaptor<List<StockReceipt>> receipts = ArgumentCaptor.forClass(List.class);
        verify(inboundStockPort).receive(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(USER), receipts.capture());
        assertThat(receipts.getValue()).containsExactly(
                new StockReceipt(2L, 31L, false, 58L), new StockReceipt(9L, 31L, true, 2L));

        assertThat(result.inbound().getStatus()).isEqualTo(InboundStatus.COMPLETED);
        assertThat(result.inbound().getReceivedBy()).isEqualTo(USER);
        assertThat(result.inbound().getReceivedAt()).isNotNull();
        assertThat(result.inventory()).containsExactly(
                new InboundCompleteResult.ReflectedInventory(21L, 101L, 58L, 105L, 2L));
        assertThat(result.purchaseOrderLines()).containsExactly(new InboundCompleteResult.PurchaseOrderLineProgress(
                11L, 100L, 60L, PurchaseOrderLineStatus.PARTIALLY_RECEIVED));
        assertThat(result.purchaseOrderId()).isEqualTo(4L);
        assertThat(result.purchaseOrderStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("모든 발주 항목이 전량 입고되면 발주도 COMPLETED로 바꾼다")
    void complete_allLinesDone_completesPurchaseOrder() {
        givenInspectingInbound(inboundLine(21L, 11L, 31L, 60, 0, 2L, null));
        givenPurchaseOrder(PurchaseOrderStatus.CONFIRMED,
                purchaseOrderLine(11L, 60, 0, PurchaseOrderLineStatus.REQUESTED),
                purchaseOrderLine(12L, 10, 10, PurchaseOrderLineStatus.COMPLETED));
        when(inboundStockPort.receive(anyLong(), anyLong(), anyList()))
                .thenReturn(List.of(new ReceivedStock(2L, 31L, false, 101L)));
        stubSaves();
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InboundCompleteResult result = inboundCompleteService.completeInbound(7L, USER);

        assertThat(result.purchaseOrderLines()).containsExactly(new InboundCompleteResult.PurchaseOrderLineProgress(
                11L, 60L, 60L, PurchaseOrderLineStatus.COMPLETED));
        assertThat(result.purchaseOrderStatus()).isEqualTo(PurchaseOrderStatus.COMPLETED);
        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("전량 입고된 항목이 있어도 다른 항목이 남았으면 발주는 CONFIRMED를 유지한다")
    void complete_otherLinePending_keepsPurchaseOrderConfirmed() {
        givenInspectingInbound(inboundLine(21L, 11L, 31L, 60, 0, 2L, null));
        givenPurchaseOrder(PurchaseOrderStatus.CONFIRMED,
                purchaseOrderLine(11L, 60, 0, PurchaseOrderLineStatus.REQUESTED),
                purchaseOrderLine(12L, 10, 0, PurchaseOrderLineStatus.REQUESTED));
        when(inboundStockPort.receive(anyLong(), anyLong(), anyList()))
                .thenReturn(List.of(new ReceivedStock(2L, 31L, false, 101L)));
        stubSaves();

        InboundCompleteResult result = inboundCompleteService.completeInbound(7L, USER);

        assertThat(result.purchaseOrderStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("합격만 있는 항목은 불량 재고를, 불량만 있는 항목은 합격 재고를 반영하지 않는다")
    void complete_onlyOneSideReflected() {
        givenInspectingInbound(
                inboundLine(21L, 11L, 31L, 50, 0, 2L, null),
                inboundLine(22L, 12L, 32L, 0, 5, null, 9L));
        givenPurchaseOrder(PurchaseOrderStatus.CONFIRMED,
                purchaseOrderLine(11L, 100, 0, PurchaseOrderLineStatus.REQUESTED),
                purchaseOrderLine(12L, 100, 0, PurchaseOrderLineStatus.REQUESTED));
        when(inboundStockPort.receive(anyLong(), anyLong(), anyList())).thenReturn(List.of(
                new ReceivedStock(2L, 31L, false, 101L), new ReceivedStock(9L, 32L, true, 105L)));
        stubSaves();

        InboundCompleteResult result = inboundCompleteService.completeInbound(7L, USER);

        ArgumentCaptor<List<StockReceipt>> receipts = ArgumentCaptor.forClass(List.class);
        verify(inboundStockPort).receive(anyLong(), anyLong(), receipts.capture());
        assertThat(receipts.getValue()).containsExactly(
                new StockReceipt(2L, 31L, false, 50L), new StockReceipt(9L, 32L, true, 5L));
        assertThat(result.inventory()).containsExactly(
                new InboundCompleteResult.ReflectedInventory(21L, 101L, 50L, null, 0L),
                new InboundCompleteResult.ReflectedInventory(22L, null, 0L, 105L, 5L));
    }

    @Test
    @DisplayName("같은 발주 항목을 나눠 받은 로트는 입고 수량 합계로 누적한다")
    void complete_splitLots_accumulatesSum() {
        givenInspectingInbound(
                inboundLine(21L, 11L, 31L, 40, 0, 2L, null),
                inboundLine(22L, 11L, 32L, 15, 5, 2L, 9L));
        givenPurchaseOrder(PurchaseOrderStatus.CONFIRMED,
                purchaseOrderLine(11L, 100, 10, PurchaseOrderLineStatus.PARTIALLY_RECEIVED));
        when(inboundStockPort.receive(anyLong(), anyLong(), anyList())).thenReturn(List.of(
                new ReceivedStock(2L, 31L, false, 101L), new ReceivedStock(2L, 32L, false, 102L),
                new ReceivedStock(9L, 32L, true, 105L)));
        stubSaves();

        InboundCompleteResult result = inboundCompleteService.completeInbound(7L, USER);

        assertThat(result.purchaseOrderLines()).containsExactly(new InboundCompleteResult.PurchaseOrderLineProgress(
                11L, 100L, 70L, PurchaseOrderLineStatus.PARTIALLY_RECEIVED));
    }

    // ---------- 실패 ----------

    @Test
    @DisplayName("부분 입고 완료는 입고 상태 이력(INSPECTING → COMPLETED)만 남기고 발주 이력은 남기지 않는다")
    void complete_partial_recordsInboundHistoryOnly() {
        givenInspectingInbound(inboundLine(21L, 11L, 31L, 58, 2, 2L, 9L));
        givenPurchaseOrder(PurchaseOrderStatus.CONFIRMED,
                purchaseOrderLine(11L, 100, 0, PurchaseOrderLineStatus.REQUESTED));
        when(inboundStockPort.receive(anyLong(), anyLong(), anyList())).thenReturn(List.of(
                new ReceivedStock(2L, 31L, false, 101L), new ReceivedStock(9L, 31L, true, 105L)));
        stubSaves();

        inboundCompleteService.completeInbound(7L, USER);

        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.INBOUND, 7L, "INSPECTING", "COMPLETED", null, USER);
        verify(statusHistoryUseCase, never()).record(
                eq(StatusHistoryEntityType.PURCHASE_ORDER), anyLong(), any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("마지막 입고로 발주가 자동 완료되면 발주 상태 이력(CONFIRMED → COMPLETED)도 남긴다")
    void complete_final_recordsPurchaseOrderHistory() {
        givenInspectingInbound(inboundLine(21L, 11L, 31L, 58, 2, 2L, 9L));
        givenPurchaseOrder(PurchaseOrderStatus.CONFIRMED,
                purchaseOrderLine(11L, 60, 0, PurchaseOrderLineStatus.REQUESTED));
        when(inboundStockPort.receive(anyLong(), anyLong(), anyList())).thenReturn(List.of(
                new ReceivedStock(2L, 31L, false, 101L), new ReceivedStock(9L, 31L, true, 105L)));
        stubSaves();
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inboundCompleteService.completeInbound(7L, USER);

        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.PURCHASE_ORDER, 4L, "CONFIRMED", "COMPLETED", null, USER);
        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.INBOUND, 7L, "INSPECTING", "COMPLETED", null, USER);
    }

    @Test
    @DisplayName("처리 사용자가 없으면 VALIDATION_ERROR를 던진다")
    void complete_userRequired() {
        assertError(() -> inboundCompleteService.completeInbound(7L, null), ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(inboundRepository, purchaseOrderRepository, inboundStockPort);
    }

    @Test
    @DisplayName("없는 입고를 완료하면 INBOUND_NOT_FOUND를 던진다")
    void complete_inboundNotFound() {
        when(inboundRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertError(() -> inboundCompleteService.completeInbound(999L, USER),
                InboundErrorCode.INBOUND_NOT_FOUND.name());
    }

    @Test
    @DisplayName("검수 중(INSPECTING)이 아닌 입고는 완료할 수 없고 재고를 건드리지 않는다")
    void complete_notInspecting() {
        for (InboundStatus status : List.of(
                InboundStatus.ARRIVED, InboundStatus.COMPLETED, InboundStatus.CANCELED)) {
            when(inboundRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(inbound(status)));

            assertError(() -> inboundCompleteService.completeInbound(7L, USER), ErrorCode.CONFLICT.name());
        }
        verifyNoInteractions(inboundStockPort, purchaseOrderRepository);
        verify(inboundRepository, never()).save(any(Inbound.class));
    }

    @Test
    @DisplayName("검수 항목이 없으면 INBOUND_HAS_NO_LINES를 던진다")
    void complete_noLines() {
        givenInspectingInbound();

        assertError(() -> inboundCompleteService.completeInbound(7L, USER),
                InboundErrorCode.INBOUND_HAS_NO_LINES.name());
        verifyNoInteractions(inboundStockPort, purchaseOrderRepository);
    }

    @Test
    @DisplayName("합격 수량이 있는데 합격 구역이, 불량 수량이 있는데 불량 구역이 없으면 SECTION_NOT_ASSIGNED를 던진다")
    void complete_sectionNotAssigned() {
        givenInspectingInbound(inboundLine(21L, 11L, 31L, 58, 2, 2L, null));

        assertError(() -> inboundCompleteService.completeInbound(7L, USER),
                InboundErrorCode.SECTION_NOT_ASSIGNED.name());
        verifyNoInteractions(inboundStockPort, purchaseOrderRepository);

        givenInspectingInbound(inboundLine(21L, 11L, 31L, 58, 2, null, 9L));

        assertError(() -> inboundCompleteService.completeInbound(7L, USER),
                InboundErrorCode.SECTION_NOT_ASSIGNED.name());
        verifyNoInteractions(inboundStockPort, purchaseOrderRepository);
    }

    @Test
    @DisplayName("입고의 발주를 찾을 수 없으면 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void complete_purchaseOrderNotFound() {
        givenInspectingInbound(inboundLine(21L, 11L, 31L, 58, 2, 2L, 9L));
        when(purchaseOrderRepository.findByIdForUpdate(4L)).thenReturn(Optional.empty());

        assertError(() -> inboundCompleteService.completeInbound(7L, USER),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
        verifyNoInteractions(inboundStockPort);
    }

    @Test
    @DisplayName("발주가 확정 상태가 아니면 CONFLICT를 던지고 재고를 건드리지 않는다")
    void complete_purchaseOrderNotConfirmed() {
        givenInspectingInbound(inboundLine(21L, 11L, 31L, 58, 2, 2L, 9L));
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(PurchaseOrderStatus.CANCELED)));

        assertError(() -> inboundCompleteService.completeInbound(7L, USER), ErrorCode.CONFLICT.name());
        verifyNoInteractions(inboundStockPort);
    }

    @Test
    @DisplayName("입고 수량 합계가 발주 항목 잔여 수량을 넘으면 CONFLICT를 던지고 입고를 완료하지 않는다")
    void complete_exceedsRemaining() {
        givenInspectingInbound(inboundLine(21L, 11L, 31L, 58, 2, 2L, 9L));
        givenPurchaseOrder(PurchaseOrderStatus.CONFIRMED,
                purchaseOrderLine(11L, 100, 50, PurchaseOrderLineStatus.PARTIALLY_RECEIVED));
        when(inboundStockPort.receive(anyLong(), anyLong(), anyList())).thenReturn(List.of(
                new ReceivedStock(2L, 31L, false, 101L), new ReceivedStock(9L, 31L, true, 105L)));

        assertError(() -> inboundCompleteService.completeInbound(7L, USER), ErrorCode.CONFLICT.name());
        verify(purchaseOrderRepository, never()).saveLines(anyList());
        verify(inboundRepository, never()).save(any(Inbound.class));
    }

    @Test
    @DisplayName("재고 반영이 실패하면 오류를 그대로 던지고 발주 항목·입고 상태를 바꾸지 않는다")
    void complete_stockFailurePropagates() {
        givenInspectingInbound(inboundLine(21L, 11L, 31L, 58, 2, 2L, 9L));
        givenPurchaseOrder(PurchaseOrderStatus.CONFIRMED,
                purchaseOrderLine(11L, 100, 0, PurchaseOrderLineStatus.REQUESTED));
        when(inboundStockPort.receive(anyLong(), anyLong(), anyList()))
                .thenThrow(new BusinessException(InventoryErrorCode.LOT_NOT_AVAILABLE));

        assertError(() -> inboundCompleteService.completeInbound(7L, USER),
                InventoryErrorCode.LOT_NOT_AVAILABLE.name());
        verify(purchaseOrderRepository, never()).saveLines(anyList());
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
        verify(inboundRepository, never()).save(any(Inbound.class));
    }
}

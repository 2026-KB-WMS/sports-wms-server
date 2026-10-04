package com.kb.wms.storeorder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.entity.StatusHistory;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRegisterCommand;
import com.kb.wms.storeorder.application.port.in.query.StoreOrderSearchCondition;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetail;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetails;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderLineView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderListItem;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderSummary;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderView;
import com.kb.wms.storeorder.application.port.out.SkuSupplyPricePort;
import com.kb.wms.storeorder.application.port.out.StoreAvailabilityPort;
import com.kb.wms.storeorder.application.port.out.StoreOrderOutboundPort;
import com.kb.wms.storeorder.application.port.out.StoreOrderQueryRepository;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.application.port.out.WarehouseExistencePort;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderProgressStage;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.storeorder.exception.StoreOrderErrorCode;

@ExtendWith(MockitoExtension.class)
class StoreOrderServiceTest {

    @Mock
    private StoreOrderRepository storeOrderRepository;

    @Mock
    private StoreOrderQueryRepository storeOrderQueryRepository;

    @Mock
    private StoreOrderOutboundPort storeOrderOutboundPort;

    @Mock
    private StoreAvailabilityPort storeAvailabilityPort;

    @Mock
    private WarehouseExistencePort warehouseExistencePort;

    @Mock
    private SkuSupplyPricePort skuSupplyPricePort;

    @Mock
    private StatusHistoryUseCase statusHistoryUseCase;

    @InjectMocks
    private StoreOrderService storeOrderService;

    // ---------- fixtures ----------

    private static StoreOrderRegisterCommand.Line line(Long skuId, long quantity) {
        return new StoreOrderRegisterCommand.Line(skuId, quantity);
    }

    private static StoreOrderRegisterCommand registerCommand(LocalDateTime deliveryAt, String note,
                                                             StoreOrderRegisterCommand.Line... lines) {
        return new StoreOrderRegisterCommand(1L, deliveryAt, note, 5L, List.of(lines));
    }

    private static StoreOrder savedOrder(Long id) {
        return StoreOrder.builder()
                .storeOrderId(id)
                .orderNo("SO-20261004-0001")
                .storeId(1L)
                .status(StoreOrderStatus.REQUESTED)
                .createdBy(5L)
                .build();
    }

    private static StoreOrderView view(Long id, StoreOrderStatus status, long shortageLineCount) {
        return new StoreOrderView(id, "SO-20261004-0001", 1L, "강남점", null, null, status,
                LocalDateTime.of(2026, 10, 4, 9, 0), null, null, 1L, new BigDecimal("1000.00"),
                shortageLineCount, 5L, LocalDateTime.of(2026, 10, 4, 9, 0), LocalDateTime.of(2026, 10, 4, 9, 0));
    }

    private static StoreOrderSummary summary(Long id, StoreOrderStatus status, long shortageLineCount) {
        return new StoreOrderSummary(id, "SO-20261004-000" + id, 1L, "강남점", null, null, status,
                LocalDateTime.of(2026, 10, 4, 9, 0), null, 1L, new BigDecimal("1000.00"), shortageLineCount);
    }

    private static StoreOrderSearchCondition condition(Long storeId, Long warehouseId,
                                                       LocalDateTime from, LocalDateTime to) {
        return new StoreOrderSearchCondition(null, storeId, warehouseId, null, from, to);
    }

    private static void assertError(ThrowingCallable call, String errorCodeName) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(errorCodeName);
    }

    // ---------- registerStoreOrder ----------

    @Test
    @DisplayName("발주를 등록하면 REQUESTED 헤더와 공급 단가 스냅샷 항목을 저장하고 이력을 남긴다")
    void registerStoreOrder_savesHeaderLinesAndHistory() {
        when(skuSupplyPricePort.getOrderableSupplyPrice(10L)).thenReturn(new BigDecimal("1500.00"));
        when(skuSupplyPricePort.getOrderableSupplyPrice(11L)).thenReturn(new BigDecimal("200.00"));
        when(storeOrderRepository.countByOrderNoPrefix(any())).thenReturn(2L);
        when(storeOrderRepository.save(any(StoreOrder.class))).thenReturn(savedOrder(77L));

        Long id = storeOrderService.registerStoreOrder(registerCommand(
                LocalDateTime.now().plusDays(1), "빠른 배송 부탁", line(10L, 3), line(11L, 5)));

        assertThat(id).isEqualTo(77L);

        ArgumentCaptor<StoreOrder> orderCaptor = ArgumentCaptor.forClass(StoreOrder.class);
        verify(storeOrderRepository).save(orderCaptor.capture());
        StoreOrder order = orderCaptor.getValue();
        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.REQUESTED);
        assertThat(order.getWarehouseId()).isNull();
        assertThat(order.getStoreId()).isEqualTo(1L);
        assertThat(order.getCreatedBy()).isEqualTo(5L);
        assertThat(order.getRequestedAt()).isNotNull();
        assertThat(order.getOrderNo()).startsWith("SO-").endsWith("-0003");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StoreOrderLine>> linesCaptor = ArgumentCaptor.forClass(List.class);
        verify(storeOrderRepository).saveLines(linesCaptor.capture());
        List<StoreOrderLine> lines = linesCaptor.getValue();
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).getStoreOrderId()).isEqualTo(77L);
        assertThat(lines.get(0).getSkuId()).isEqualTo(10L);
        assertThat(lines.get(0).getRequestedQuantity()).isEqualTo(3L);
        assertThat(lines.get(0).getRequestedUnitSupplyPrice()).isEqualByComparingTo("1500.00");
        assertThat(lines.get(0).getStatus()).isEqualTo(StoreOrderLineStatus.REQUESTED);
        assertThat(lines.get(0).getAllocatedQuantity()).isZero();
        assertThat(lines.get(0).getShippedQuantity()).isZero();
        assertThat(lines.get(1).getRequestedUnitSupplyPrice()).isEqualByComparingTo("200.00");

        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.STORE_ORDER, 77L, null, "REQUESTED", null, 5L);
    }

    @Test
    @DisplayName("항목이 비어 있으면 400 VALIDATION_ERROR")
    void registerStoreOrder_emptyLines() {
        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(null, null)), "VALIDATION_ERROR");
        verifyNoInteractions(storeAvailabilityPort, storeOrderRepository);
    }

    @Test
    @DisplayName("수량이 1 미만이면 400 VALIDATION_ERROR")
    void registerStoreOrder_quantityBelowOne() {
        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(null, null, line(10L, 0))),
                "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderRepository);
    }

    @Test
    @DisplayName("같은 SKU가 두 번 들어오면 400 VALIDATION_ERROR")
    void registerStoreOrder_duplicateSku() {
        assertError(() -> storeOrderService.registerStoreOrder(
                registerCommand(null, null, line(10L, 1), line(10L, 2))), "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderRepository);
    }

    @Test
    @DisplayName("요청 배송 일시가 과거이면 400 VALIDATION_ERROR")
    void registerStoreOrder_pastDeliveryAt() {
        assertError(() -> storeOrderService.registerStoreOrder(
                registerCommand(LocalDateTime.now().minusMinutes(1), null, line(10L, 1))), "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderRepository);
    }

    @Test
    @DisplayName("비고가 1000자를 넘으면 400 VALIDATION_ERROR")
    void registerStoreOrder_noteTooLong() {
        assertError(() -> storeOrderService.registerStoreOrder(
                registerCommand(null, "a".repeat(1001), line(10L, 1))), "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderRepository);
    }

    @Test
    @DisplayName("지점이 없거나 비활성이면 지점 포트의 예외가 그대로 전파되고 아무것도 저장하지 않는다")
    void registerStoreOrder_storeNotAvailable() {
        doThrow(new BusinessException(com.kb.wms.store.exception.StoreErrorCode.STORE_NOT_FOUND))
                .when(storeAvailabilityPort).requireActive(1L);

        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(null, null, line(10L, 1))),
                "STORE_NOT_FOUND");
        verifyNoInteractions(storeOrderRepository, skuSupplyPricePort, statusHistoryUseCase);
    }

    @Test
    @DisplayName("공급 단가가 없는 SKU면 409 SUPPLY_PRICE_MISSING이고 아무것도 저장하지 않는다")
    void registerStoreOrder_supplyPriceMissing() {
        when(skuSupplyPricePort.getOrderableSupplyPrice(10L))
                .thenThrow(new BusinessException(StoreOrderErrorCode.SUPPLY_PRICE_MISSING));

        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(null, null, line(10L, 1))),
                "SUPPLY_PRICE_MISSING");
        verify(storeOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    // ---------- getStoreOrders ----------

    @Test
    @DisplayName("요청 시작 일시가 종료 일시보다 늦으면 400 VALIDATION_ERROR")
    void getStoreOrders_invalidRange() {
        LocalDateTime now = LocalDateTime.now();
        assertError(() -> storeOrderService.getStoreOrders(condition(null, null, now, now.minusDays(1))),
                "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderQueryRepository);
    }

    @Test
    @DisplayName("존재하지 않는 지점·창고 필터면 각 포트의 404가 전파된다")
    void getStoreOrders_unknownFilters() {
        doThrow(new BusinessException(com.kb.wms.store.exception.StoreErrorCode.STORE_NOT_FOUND))
                .when(storeAvailabilityPort).requireExists(99L);
        assertError(() -> storeOrderService.getStoreOrders(condition(99L, null, null, null)), "STORE_NOT_FOUND");

        doThrow(new BusinessException(com.kb.wms.common.exception.ErrorCode.NOT_FOUND, "창고 없음"))
                .when(warehouseExistencePort).requireExists(88L);
        assertError(() -> storeOrderService.getStoreOrders(condition(null, 88L, null, null)), "NOT_FOUND");
    }

    @Test
    @DisplayName("목록은 최근 출고 상태와 진행 단계를 함께 채운다")
    void getStoreOrders_fillsProgressStage() {
        StoreOrderSearchCondition condition = condition(null, null, null, null);
        when(storeOrderQueryRepository.search(condition)).thenReturn(List.of(
                summary(1L, StoreOrderStatus.REQUESTED, 0),
                summary(2L, StoreOrderStatus.ASSIGNED, 1),
                summary(3L, StoreOrderStatus.COMPLETED, 1)));
        when(storeOrderOutboundPort.findLatestOutboundStatuses(List.of(1L, 2L, 3L)))
                .thenReturn(Map.of(2L, StoreOrderOutboundStatus.SHIPPED));

        List<StoreOrderListItem> items = storeOrderService.getStoreOrders(condition);

        assertThat(items).extracting(StoreOrderListItem::progressStage).containsExactly(
                StoreOrderProgressStage.PENDING_APPROVAL,
                StoreOrderProgressStage.IN_TRANSIT,
                StoreOrderProgressStage.COMPLETED_PARTIAL);
        assertThat(items.get(0).latestOutboundStatus()).isNull();
        assertThat(items.get(1).latestOutboundStatus()).isEqualTo(StoreOrderOutboundStatus.SHIPPED);
    }

    // ---------- getStoreOrder ----------

    @Test
    @DisplayName("없는 발주를 조회하면 404 STORE_ORDER_NOT_FOUND")
    void getStoreOrder_notFound() {
        when(storeOrderQueryRepository.findView(anyLong())).thenReturn(Optional.empty());

        assertError(() -> storeOrderService.getStoreOrder(1L), "STORE_ORDER_NOT_FOUND");
        assertError(() -> storeOrderService.getStoreOrderDetails(1L), "STORE_ORDER_NOT_FOUND");
    }

    @Test
    @DisplayName("반려된 발주는 이력의 사유를 statusReason으로 내려준다")
    void getStoreOrder_rejectedHasReason() {
        when(storeOrderQueryRepository.findView(5L)).thenReturn(Optional.of(view(5L, StoreOrderStatus.REJECTED, 0)));
        when(storeOrderOutboundPort.findLatestOutboundStatus(5L)).thenReturn(Optional.empty());
        when(statusHistoryUseCase.findStatusReason(StatusHistoryEntityType.STORE_ORDER, 5L, "REJECTED"))
                .thenReturn(Optional.of("단가 협의 필요"));

        StoreOrderDetail detail = storeOrderService.getStoreOrder(5L);

        assertThat(detail.statusReason()).isEqualTo("단가 협의 필요");
        assertThat(detail.progressStage()).isEqualTo(StoreOrderProgressStage.REJECTED);
    }

    @Test
    @DisplayName("재개 후 ASSIGNED처럼 사유가 필요 없는 상태는 이력을 조회하지 않고 statusReason이 null이다")
    void getStoreOrder_assignedHasNoReason() {
        when(storeOrderQueryRepository.findView(6L)).thenReturn(Optional.of(view(6L, StoreOrderStatus.ASSIGNED, 0)));
        when(storeOrderOutboundPort.findLatestOutboundStatus(6L)).thenReturn(Optional.empty());

        StoreOrderDetail detail = storeOrderService.getStoreOrder(6L);

        assertThat(detail.statusReason()).isNull();
        assertThat(detail.progressStage()).isEqualTo(StoreOrderProgressStage.PREPARING);
        verifyNoInteractions(statusHistoryUseCase);
    }

    // ---------- getStoreOrderDetails ----------

    @Test
    @DisplayName("상세는 항목·출고·상태 이력을 모아 내려준다")
    void getStoreOrderDetails_assemblesItemsOutboundsHistory() {
        when(storeOrderQueryRepository.findView(7L)).thenReturn(Optional.of(view(7L, StoreOrderStatus.APPROVED, 0)));
        when(storeOrderOutboundPort.findLatestOutboundStatus(7L)).thenReturn(Optional.empty());
        StoreOrderLineView lineView = new StoreOrderLineView(1L, 10L, "SKU-10", "러닝화", "EA",
                3L, 0L, 0L, new BigDecimal("1500.00"), StoreOrderLineStatus.REQUESTED);
        when(storeOrderQueryRepository.findLineViews(7L)).thenReturn(List.of(lineView));
        when(storeOrderOutboundPort.findOutbounds(7L)).thenReturn(List.of());
        LocalDateTime at = LocalDateTime.of(2026, 10, 4, 9, 0);
        when(statusHistoryUseCase.findHistory(StatusHistoryEntityType.STORE_ORDER, 7L)).thenReturn(List.of(
                StatusHistory.record(StatusHistoryEntityType.STORE_ORDER, 7L, null, "REQUESTED", null, 5L, at),
                StatusHistory.record(StatusHistoryEntityType.STORE_ORDER, 7L, "REQUESTED", "APPROVED", null, 2L, at)));

        StoreOrderDetails details = storeOrderService.getStoreOrderDetails(7L);

        assertThat(details.storeOrderId()).isEqualTo(7L);
        assertThat(details.status()).isEqualTo(StoreOrderStatus.APPROVED);
        assertThat(details.progressStage()).isEqualTo(StoreOrderProgressStage.AWAITING_ASSIGNMENT);
        assertThat(details.items()).containsExactly(lineView);
        assertThat(details.outbounds()).isEmpty();
        assertThat(details.statusHistory()).hasSize(2);
        assertThat(details.statusHistory().get(0).fromStatus()).isNull();
        assertThat(details.statusHistory().get(0).toStatus()).isEqualTo("REQUESTED");
        assertThat(details.statusHistory().get(1).changedBy()).isEqualTo(2L);
    }
}

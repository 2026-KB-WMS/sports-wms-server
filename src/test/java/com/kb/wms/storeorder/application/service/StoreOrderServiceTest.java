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

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.statushistory.domain.entity.StatusHistory;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderHoldCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderResumeCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRegisterCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCancelCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRejectCommand;
import com.kb.wms.storeorder.application.port.in.query.StoreOrderSearchCondition;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetail;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetails;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderFulfillmentCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderLineView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderListItem;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderStatusChange;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderSummary;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderView;
import com.kb.wms.storeorder.application.port.out.SkuAvailabilityPort;
import com.kb.wms.storeorder.application.port.out.SkuSupplyPricePort;
import com.kb.wms.storeorder.application.port.out.StoreAvailabilityPort;
import com.kb.wms.storeorder.application.port.out.StoreOrderOutboundPort;
import com.kb.wms.storeorder.application.port.out.StoreOrderQueryRepository;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.application.port.out.WarehouseAvailabilityPort;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderProgressStage;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.storeorder.exception.StoreOrderErrorCode;

@ExtendWith(MockitoExtension.class)
class StoreOrderServiceTest {

    // 발주 5번 사용자가 지점 1에서 만든 요청 발주의 작성자(점주)
    private static final AuthenticatedUser AUTHOR =
            new AuthenticatedUser(5L, UserRole.STORE_OWNER, List.of(), List.of(1L));
    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Mock
    private StoreOrderRepository storeOrderRepository;

    @Mock
    private StoreOrderQueryRepository storeOrderQueryRepository;

    @Mock
    private StoreOrderOutboundPort storeOrderOutboundPort;

    @Mock
    private StoreAvailabilityPort storeAvailabilityPort;

    @Mock
    private WarehouseAvailabilityPort warehouseAvailabilityPort;

    @Mock
    private SkuSupplyPricePort skuSupplyPricePort;

    @Mock
    private SkuAvailabilityPort skuAvailabilityPort;

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
                shortageLineCount, 5L, null, LocalDateTime.of(2026, 10, 4, 9, 0), LocalDateTime.of(2026, 10, 4, 9, 0));
    }

    private static StoreOrderSummary summary(Long id, StoreOrderStatus status, long shortageLineCount) {
        return new StoreOrderSummary(id, "SO-20261004-000" + id, 1L, "강남점", null, null, status,
                LocalDateTime.of(2026, 10, 4, 9, 0), null, 1L, new BigDecimal("1000.00"), shortageLineCount);
    }

    private static StoreOrderSearchCondition condition(Long storeId, Long warehouseId,
                                                       LocalDateTime from, LocalDateTime to) {
        return StoreOrderSearchCondition.unscoped(null, storeId, warehouseId, null, from, to);
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
                LocalDateTime.now().plusDays(1), "빠른 배송 부탁", line(10L, 3), line(11L, 5)), HQ);

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
        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(null, null), HQ), "VALIDATION_ERROR");
        verifyNoInteractions(storeAvailabilityPort, storeOrderRepository);
    }

    @Test
    @DisplayName("수량이 1 미만이면 400 VALIDATION_ERROR")
    void registerStoreOrder_quantityBelowOne() {
        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(null, null, line(10L, 0)), HQ),
                "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderRepository);
    }

    @Test
    @DisplayName("같은 SKU가 두 번 들어오면 400 VALIDATION_ERROR")
    void registerStoreOrder_duplicateSku() {
        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(null, null, line(10L, 1), line(10L, 2)), HQ), "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderRepository);
    }

    @Test
    @DisplayName("요청 배송 일시가 과거이면 400 VALIDATION_ERROR")
    void registerStoreOrder_pastDeliveryAt() {
        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(LocalDateTime.now().minusMinutes(1), null, line(10L, 1)), HQ), "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderRepository);
    }

    @Test
    @DisplayName("비고가 1000자를 넘으면 400 VALIDATION_ERROR")
    void registerStoreOrder_noteTooLong() {
        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(null, "a".repeat(1001), line(10L, 1)), HQ), "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderRepository);
    }

    @Test
    @DisplayName("지점이 없거나 비활성이면 지점 포트의 예외가 그대로 전파되고 아무것도 저장하지 않는다")
    void registerStoreOrder_storeNotAvailable() {
        doThrow(new BusinessException(com.kb.wms.store.exception.StoreErrorCode.STORE_NOT_FOUND))
                .when(storeAvailabilityPort).requireActive(1L);

        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(null, null, line(10L, 1)), HQ),
                "STORE_NOT_FOUND");
        verifyNoInteractions(storeOrderRepository, skuSupplyPricePort, statusHistoryUseCase);
    }

    @Test
    @DisplayName("공급 단가가 없는 SKU면 409 SUPPLY_PRICE_MISSING이고 아무것도 저장하지 않는다")
    void registerStoreOrder_supplyPriceMissing() {
        when(skuSupplyPricePort.getOrderableSupplyPrice(10L))
                .thenThrow(new BusinessException(StoreOrderErrorCode.SUPPLY_PRICE_MISSING));

        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(null, null, line(10L, 1)), HQ),
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
                .when(warehouseAvailabilityPort).requireExists(88L);
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

    // ---------- getMyStoreOrders ----------

    @Test
    @DisplayName("내 발주 목록: 점주는 필터가 없으면 담당 지점으로, 창고 관리자는 담당 창고로 범위를 좁힌다")
    void getMyStoreOrders_scopedToAssigned() {
        StoreOrderSearchCondition none = condition(null, null, null, null);
        StoreOrderSearchCondition ownerScope = new StoreOrderSearchCondition(
                null, null, null, null, null, null, List.of(1L, 2L), null);
        StoreOrderSearchCondition managerScope = new StoreOrderSearchCondition(
                null, null, null, null, null, null, null, List.of(3L));
        when(storeOrderQueryRepository.search(ownerScope)).thenReturn(List.of(summary(1L, StoreOrderStatus.REQUESTED, 0)));
        when(storeOrderQueryRepository.search(managerScope)).thenReturn(List.of(summary(2L, StoreOrderStatus.ASSIGNED, 0)));

        AuthenticatedUser storeOwner = new AuthenticatedUser(5L, UserRole.STORE_OWNER, List.of(), List.of(1L, 2L));
        assertThat(storeOrderService.getMyStoreOrders(none, storeOwner))
                .extracting(item -> item.summary().storeOrderId()).containsExactly(1L);
        assertThat(storeOrderService.getMyStoreOrders(none, manager(7L, 3L)))
                .extracting(item -> item.summary().storeOrderId()).containsExactly(2L);
    }

    @Test
    @DisplayName("내 발주 목록: 담당 지점·창고를 필터로 지정하면 추가 범위 없이 그 조건으로만 조회한다")
    void getMyStoreOrders_specifiedFilterWithinScope() {
        when(storeOrderQueryRepository.search(condition(1L, null, null, null))).thenReturn(List.of());
        when(storeOrderQueryRepository.search(condition(null, 3L, null, null))).thenReturn(List.of());

        assertThat(storeOrderService.getMyStoreOrders(condition(1L, null, null, null), owner(5L, 1L))).isEmpty();
        assertThat(storeOrderService.getMyStoreOrders(condition(null, 3L, null, null), manager(7L, 3L))).isEmpty();
    }

    @Test
    @DisplayName("내 발주 목록: 담당이 아닌 지점·창고 필터와 본사 호출은 403이고, 존재 확인·조회보다 먼저 거절한다")
    void getMyStoreOrders_forbidden() {
        assertError(() -> storeOrderService.getMyStoreOrders(condition(2L, null, null, null), owner(5L, 1L)), "FORBIDDEN");
        assertError(() -> storeOrderService.getMyStoreOrders(condition(null, 9L, null, null), manager(7L, 3L)), "FORBIDDEN");
        assertError(() -> storeOrderService.getMyStoreOrders(condition(null, null, null, null), HQ), "FORBIDDEN");
        verifyNoInteractions(storeOrderQueryRepository, storeAvailabilityPort, warehouseAvailabilityPort);
    }

    // ---------- getStoreOrder ----------

    @Test
    @DisplayName("없는 발주를 조회하면 404 STORE_ORDER_NOT_FOUND")
    void getStoreOrder_notFound() {
        when(storeOrderQueryRepository.findView(anyLong())).thenReturn(Optional.empty());

        assertError(() -> storeOrderService.getStoreOrder(1L, HQ), "STORE_ORDER_NOT_FOUND");
        assertError(() -> storeOrderService.getStoreOrderDetails(1L, HQ), "STORE_ORDER_NOT_FOUND");
    }

    @Test
    @DisplayName("반려된 발주는 이력의 사유를 statusReason으로 내려준다")
    void getStoreOrder_rejectedHasReason() {
        when(storeOrderQueryRepository.findView(5L)).thenReturn(Optional.of(view(5L, StoreOrderStatus.REJECTED, 0)));
        when(storeOrderOutboundPort.findLatestOutboundStatus(5L)).thenReturn(Optional.empty());
        when(statusHistoryUseCase.findStatusReason(StatusHistoryEntityType.STORE_ORDER, 5L, "REJECTED"))
                .thenReturn(Optional.of("단가 협의 필요"));

        StoreOrderDetail detail = storeOrderService.getStoreOrder(5L, HQ);

        assertThat(detail.statusReason()).isEqualTo("단가 협의 필요");
        assertThat(detail.progressStage()).isEqualTo(StoreOrderProgressStage.REJECTED);
    }

    @Test
    @DisplayName("재개 후 ASSIGNED처럼 사유가 필요 없는 상태는 이력을 조회하지 않고 statusReason이 null이다")
    void getStoreOrder_assignedHasNoReason() {
        when(storeOrderQueryRepository.findView(6L)).thenReturn(Optional.of(view(6L, StoreOrderStatus.ASSIGNED, 0)));
        when(storeOrderOutboundPort.findLatestOutboundStatus(6L)).thenReturn(Optional.empty());

        StoreOrderDetail detail = storeOrderService.getStoreOrder(6L, HQ);

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

        StoreOrderDetails details = storeOrderService.getStoreOrderDetails(7L, HQ);

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

    // ---------- 승인·반려·취소 fixtures ----------

    private static StoreOrder orderIn(Long id, StoreOrderStatus status) {
        return StoreOrder.builder()
                .storeOrderId(id).orderNo("SO-20261004-0001").storeId(1L).warehouseId(
                        status == StoreOrderStatus.REQUESTED || status == StoreOrderStatus.APPROVED ? null : 3L)
                .status(status).createdBy(5L).build();
    }

    private static StoreOrderLine requestedLine(Long storeOrderId, Long skuId) {
        return StoreOrderLine.register(storeOrderId, skuId, 3, new BigDecimal("1500.00"));
    }

    /** 부분 종결은 락 순서(할당 → 항목)를 지키려고 항목을 잠그지 않고 읽는다. */
    private void givenLinesUnlocked(Long storeOrderId, StoreOrderLine... lines) {
        when(storeOrderRepository.findLinesByStoreOrderId(storeOrderId)).thenReturn(List.of(lines));
    }

    private void givenLockedOrder(Long id, StoreOrderStatus status) {
        when(storeOrderRepository.findByIdForUpdate(id)).thenReturn(Optional.of(orderIn(id, status)));
        org.mockito.Mockito.lenient().when(storeOrderRepository.save(any(StoreOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void givenLines(Long id, StoreOrderLine... lines) {
        when(storeOrderRepository.findLinesByStoreOrderIdForUpdate(id)).thenReturn(List.of(lines));
    }

    @SuppressWarnings("unchecked")
    private List<StoreOrderLine> capturedSavedLines() {
        ArgumentCaptor<List<StoreOrderLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(storeOrderRepository).saveLines(captor.capture());
        return captor.getValue();
    }

    // ---------- approveStoreOrder ----------

    @Test
    @DisplayName("승인하면 APPROVED로 바뀌고 창고는 비워 둔 채 이력을 남긴다")
    void approveStoreOrder_success() {
        givenLockedOrder(1L, StoreOrderStatus.REQUESTED);
        givenLines(1L, requestedLine(1L, 10L), requestedLine(1L, 11L));
        LocalDateTime updatedAt = LocalDateTime.of(2026, 10, 4, 10, 0);
        when(storeOrderQueryRepository.findView(1L)).thenReturn(Optional.of(
                new StoreOrderView(1L, "SO-20261004-0001", 1L, "강남점", null, null, StoreOrderStatus.APPROVED,
                        updatedAt, null, null, 2L, new BigDecimal("9000.00"), 2L, 5L, null, updatedAt, updatedAt)));

        StoreOrderStatusChange result = storeOrderService.approveStoreOrder(1L, 2L);

        assertThat(result.storeOrderId()).isEqualTo(1L);
        assertThat(result.orderNo()).isEqualTo("SO-20261004-0001");
        assertThat(result.status()).isEqualTo(StoreOrderStatus.APPROVED);
        assertThat(result.statusReason()).isNull();
        assertThat(result.updatedAt()).isEqualTo(updatedAt);

        ArgumentCaptor<StoreOrder> captor = ArgumentCaptor.forClass(StoreOrder.class);
        verify(storeOrderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StoreOrderStatus.APPROVED);
        assertThat(captor.getValue().getWarehouseId()).isNull();

        verify(storeAvailabilityPort).requireActive(1L);
        verify(skuAvailabilityPort).requireActive(10L);
        verify(skuAvailabilityPort).requireActive(11L);
        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.STORE_ORDER, 1L, "REQUESTED", "APPROVED", null, 2L);
    }

    @Test
    @DisplayName("없는 발주를 승인하면 404 STORE_ORDER_NOT_FOUND")
    void approveStoreOrder_notFound() {
        when(storeOrderRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());

        assertError(() -> storeOrderService.approveStoreOrder(9L, 2L), "STORE_ORDER_NOT_FOUND");
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("REQUESTED가 아닌 발주를 승인하면 409 CONFLICT이고 부작용이 없다")
    void approveStoreOrder_notRequested() {
        for (StoreOrderStatus status : List.of(StoreOrderStatus.APPROVED, StoreOrderStatus.REJECTED,
                StoreOrderStatus.CANCELED, StoreOrderStatus.ASSIGNED, StoreOrderStatus.COMPLETED)) {
            when(storeOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(orderIn(1L, status)));

            assertError(() -> storeOrderService.approveStoreOrder(1L, 2L), "CONFLICT");
        }
        verify(storeOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase, skuAvailabilityPort);
    }

    @Test
    @DisplayName("지점이 비활성이면 승인하지 않는다")
    void approveStoreOrder_inactiveStore() {
        givenLockedOrder(1L, StoreOrderStatus.REQUESTED);
        doThrow(new BusinessException(com.kb.wms.common.exception.ErrorCode.CONFLICT, "비활성 지점"))
                .when(storeAvailabilityPort).requireActive(1L);

        assertError(() -> storeOrderService.approveStoreOrder(1L, 2L), "CONFLICT");
        verify(storeOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("항목 SKU가 비활성이면 승인하지 않는다")
    void approveStoreOrder_inactiveSku() {
        givenLockedOrder(1L, StoreOrderStatus.REQUESTED);
        givenLines(1L, requestedLine(1L, 10L), requestedLine(1L, 11L));
        // 10L은 통과(기본 no-op), 11L만 예외. 인자가 다른 호출을 허용하려고 lenient로 둔다.
        org.mockito.Mockito.lenient()
                .doThrow(new BusinessException(com.kb.wms.common.exception.ErrorCode.CONFLICT, "비활성 SKU"))
                .when(skuAvailabilityPort).requireActive(11L);

        assertError(() -> storeOrderService.approveStoreOrder(1L, 2L), "CONFLICT");
        verify(storeOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("처리 사용자가 없으면 400 VALIDATION_ERROR")
    void approveStoreOrder_requiresChangedBy() {
        assertError(() -> storeOrderService.approveStoreOrder(1L, null), "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderRepository);
    }

    // ---------- rejectStoreOrder ----------

    @Test
    @DisplayName("반려하면 REJECTED로 바뀌고 모든 항목이 CANCELED가 되며 사유를 이력에 남긴다")
    void rejectStoreOrder_success() {
        givenLockedOrder(1L, StoreOrderStatus.REQUESTED);
        givenLines(1L, requestedLine(1L, 10L), requestedLine(1L, 11L));

        StoreOrderStatusChange result = storeOrderService.rejectStoreOrder(
                new StoreOrderRejectCommand(1L, "  단가 협의 필요  ", 2L));

        assertThat(result.status()).isEqualTo(StoreOrderStatus.REJECTED);
        assertThat(result.statusReason()).isEqualTo("단가 협의 필요");
        assertThat(capturedSavedLines()).extracting(StoreOrderLine::getStatus)
                .containsOnly(StoreOrderLineStatus.CANCELED);
        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.STORE_ORDER, 1L, "REQUESTED", "REJECTED", "단가 협의 필요", 2L);
    }

    @Test
    @DisplayName("반려 사유가 없거나 공백이거나 500자를 넘으면 400 VALIDATION_ERROR이고 발주를 조회하지 않는다")
    void rejectStoreOrder_reasonValidation() {
        assertError(() -> storeOrderService.rejectStoreOrder(new StoreOrderRejectCommand(1L, null, 2L)),
                "VALIDATION_ERROR");
        assertError(() -> storeOrderService.rejectStoreOrder(new StoreOrderRejectCommand(1L, "   ", 2L)),
                "VALIDATION_ERROR");
        assertError(() -> storeOrderService.rejectStoreOrder(
                new StoreOrderRejectCommand(1L, "a".repeat(501), 2L)), "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderRepository);
    }

    @Test
    @DisplayName("500자 사유는 허용한다")
    void rejectStoreOrder_reasonAtLimit() {
        givenLockedOrder(1L, StoreOrderStatus.REQUESTED);
        givenLines(1L, requestedLine(1L, 10L));

        StoreOrderStatusChange result = storeOrderService.rejectStoreOrder(
                new StoreOrderRejectCommand(1L, "a".repeat(500), 2L));

        assertThat(result.statusReason()).hasSize(500);
    }

    @Test
    @DisplayName("없는 발주를 반려하면 404, REQUESTED가 아니면 409 CONFLICT이고 부작용이 없다")
    void rejectStoreOrder_notFoundOrNotRequested() {
        when(storeOrderRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());
        assertError(() -> storeOrderService.rejectStoreOrder(new StoreOrderRejectCommand(9L, "사유", 2L)),
                "STORE_ORDER_NOT_FOUND");

        when(storeOrderRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(orderIn(1L, StoreOrderStatus.APPROVED)));
        assertError(() -> storeOrderService.rejectStoreOrder(new StoreOrderRejectCommand(1L, "사유", 2L)),
                "CONFLICT");

        verify(storeOrderRepository, never()).save(any());
        verify(storeOrderRepository, never()).saveLines(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    // ---------- cancelStoreOrder ----------

    @Test
    @DisplayName("승인 전 취소는 사유 없이도 되고 출고 정리 없이 항목을 모두 CANCELED로 바꾼다")
    void cancelStoreOrder_requestedWithoutReason() {
        givenLockedOrder(1L, StoreOrderStatus.REQUESTED);
        givenLines(1L, requestedLine(1L, 10L));
        when(storeOrderOutboundPort.existsPickingStarted(1L)).thenReturn(false);

        StoreOrderCancelResult result = storeOrderService.cancelStoreOrder(new StoreOrderCancelCommand(1L, null, 5L), AUTHOR);

        assertThat(result.status()).isEqualTo(StoreOrderStatus.CANCELED);
        assertThat(result.statusReason()).isNull();
        assertThat(result.releasedAllocationCount()).isZero();
        assertThat(result.canceledOutboundCount()).isZero();
        assertThat(capturedSavedLines()).extracting(StoreOrderLine::getStatus)
                .containsOnly(StoreOrderLineStatus.CANCELED);
        verify(storeOrderOutboundPort, never()).cancelFulfillment(anyLong(), anyLong());
        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.STORE_ORDER, 1L, "REQUESTED", "CANCELED", null, 5L);
    }

    @Test
    @DisplayName("승인 이후 취소는 출고 연동 포트로 할당 해제·READY 출고 취소를 하고 건수를 돌려준다")
    void cancelStoreOrder_afterApprovalReleasesFulfillment() {
        for (StoreOrderStatus status : List.of(
                StoreOrderStatus.APPROVED, StoreOrderStatus.ASSIGNED, StoreOrderStatus.ON_HOLD)) {
            givenLockedOrder(1L, status);
            givenLines(1L, requestedLine(1L, 10L));
            when(storeOrderOutboundPort.existsPickingStarted(1L)).thenReturn(false);
            when(storeOrderOutboundPort.cancelFulfillment(1L, 2L))
                    .thenReturn(new StoreOrderFulfillmentCancelResult(2, 1));

            StoreOrderCancelResult result = storeOrderService.cancelStoreOrder(new StoreOrderCancelCommand(1L, "재고 이슈", 2L), HQ);

            assertThat(result.status()).isEqualTo(StoreOrderStatus.CANCELED);
            assertThat(result.statusReason()).isEqualTo("재고 이슈");
            assertThat(result.releasedAllocationCount()).isEqualTo(2);
            assertThat(result.canceledOutboundCount()).isEqualTo(1);
            verify(statusHistoryUseCase).record(
                    StatusHistoryEntityType.STORE_ORDER, 1L, status.name(), "CANCELED", "재고 이슈", 2L);
            org.mockito.Mockito.clearInvocations(statusHistoryUseCase, storeOrderRepository);
        }
    }

    @Test
    @DisplayName("승인 이후 취소에 사유가 없으면 400 VALIDATION_ERROR이고 아무것도 바꾸지 않는다")
    void cancelStoreOrder_afterApprovalRequiresReason() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);

        assertError(() -> storeOrderService.cancelStoreOrder(new StoreOrderCancelCommand(1L, "  ", 2L), HQ),
                "VALIDATION_ERROR");
        verify(storeOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
        verify(storeOrderOutboundPort, never()).cancelFulfillment(anyLong(), anyLong());
    }

    @Test
    @DisplayName("사유가 500자를 넘으면 400 VALIDATION_ERROR")
    void cancelStoreOrder_reasonTooLong() {
        assertError(() -> storeOrderService.cancelStoreOrder(new StoreOrderCancelCommand(1L, "a".repeat(501), 5L), AUTHOR), "VALIDATION_ERROR");
        verifyNoInteractions(storeOrderRepository);
    }

    @Test
    @DisplayName("이미 취소·반려·완료된 발주를 취소하면 409 CONFLICT이고 부작용을 다시 실행하지 않는다")
    void cancelStoreOrder_alreadyTerminal() {
        for (StoreOrderStatus status : List.of(
                StoreOrderStatus.CANCELED, StoreOrderStatus.REJECTED, StoreOrderStatus.COMPLETED)) {
            when(storeOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(orderIn(1L, status)));

            assertError(() -> storeOrderService.cancelStoreOrder(new StoreOrderCancelCommand(1L, "사유", 2L), HQ),
                    "CONFLICT");
        }
        verify(storeOrderRepository, never()).save(any());
        verify(storeOrderOutboundPort, never()).cancelFulfillment(anyLong(), anyLong());
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("피킹이 시작된 출고가 있으면 409 ORDER_IN_PICKING이고 출고 정리도 하지 않는다")
    void cancelStoreOrder_orderInPicking() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);
        when(storeOrderOutboundPort.existsPickingStarted(1L)).thenReturn(true);

        assertError(() -> storeOrderService.cancelStoreOrder(new StoreOrderCancelCommand(1L, "사유", 2L), HQ),
                "ORDER_IN_PICKING");
        verify(storeOrderOutboundPort, never()).cancelFulfillment(anyLong(), anyLong());
        verify(storeOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("없는 발주를 취소하면 404 STORE_ORDER_NOT_FOUND")
    void cancelStoreOrder_notFound() {
        when(storeOrderRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());

        assertError(() -> storeOrderService.cancelStoreOrder(new StoreOrderCancelCommand(9L, "사유", 2L), HQ),
                "STORE_ORDER_NOT_FOUND");
    }

    // ---------- assignStoreOrder ----------

    private static StoreOrderLine shippedLine(Long storeOrderId, Long skuId, long requested, long shipped) {
        return StoreOrderLine.builder().storeOrderId(storeOrderId).skuId(skuId)
                .requestedQuantity(requested).shippedQuantity(shipped)
                .requestedUnitSupplyPrice(new BigDecimal("1500.00")).status(StoreOrderLineStatus.REQUESTED).build();
    }

    private void givenAssignedView(Long id, Long warehouseId, String warehouseName, StoreOrderStatus status,
                                   LocalDateTime updatedAt) {
        when(storeOrderQueryRepository.findView(id)).thenReturn(Optional.of(
                new StoreOrderView(id, "SO-20261004-0001", 1L, "강남점", warehouseId, warehouseName, status,
                        updatedAt, null, null, 1L, new BigDecimal("4500.00"), 1L, 5L, null, updatedAt, updatedAt)));
    }

    @Test
    @DisplayName("승인된 발주를 배정하면 ASSIGNED가 되고 창고가 채워지며 이력을 남긴다")
    void assignStoreOrder_firstAssignment() {
        givenLockedOrder(1L, StoreOrderStatus.APPROVED);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 10, 4, 11, 0);
        givenAssignedView(1L, 3L, "서울 물류센터", StoreOrderStatus.ASSIGNED, updatedAt);

        var result = storeOrderService.assignStoreOrder(
                new com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand(1L, 3L, null, 2L));

        assertThat(result.status()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(result.warehouseId()).isEqualTo(3L);
        assertThat(result.warehouseName()).isEqualTo("서울 물류센터");
        assertThat(result.updatedAt()).isEqualTo(updatedAt);
        verify(warehouseAvailabilityPort).requireActive(3L);
        verify(storeOrderOutboundPort, never()).existsActiveFulfillment(anyLong());
        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.STORE_ORDER, 1L, "APPROVED", "ASSIGNED", null, 2L);
    }

    @Test
    @DisplayName("ASSIGNED 발주를 다른 창고로 재배정하면 사유에 이전·이후 창고를 남기고 ASSIGNED → ASSIGNED로 기록한다")
    void assignStoreOrder_reassign() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);
        when(storeOrderOutboundPort.existsActiveFulfillment(1L)).thenReturn(false);

        var result = storeOrderService.assignStoreOrder(
                new com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand(1L, 4L, "재고 부족", 2L));

        assertThat(result.status()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(result.warehouseId()).isEqualTo(4L);
        verify(statusHistoryUseCase).record(StatusHistoryEntityType.STORE_ORDER, 1L, "ASSIGNED", "ASSIGNED",
                "창고 변경 3 → 4: 재고 부족", 2L);
    }

    @Test
    @DisplayName("재배정 이력 사유가 500자를 넘으면 사용자 사유 끝을 줄여 500자 안에 맞춘다")
    void assignStoreOrder_reassignReasonFitsHistoryLimit() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);

        storeOrderService.assignStoreOrder(
                new com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand(
                        1L, 4L, "가".repeat(500), 2L));

        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(statusHistoryUseCase).record(any(), anyLong(), any(), any(), reasonCaptor.capture(), anyLong());
        assertThat(reasonCaptor.getValue()).hasSize(500).startsWith("창고 변경 3 → 4: ").endsWith("…");
    }

    @Test
    @DisplayName("재배정에 사유가 없으면 400 VALIDATION_ERROR")
    void assignStoreOrder_reassignRequiresReason() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);

        assertError(() -> storeOrderService.assignStoreOrder(
                new com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand(1L, 4L, " ", 2L)),
                "VALIDATION_ERROR");
        verify(storeOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("현재와 같은 창고로 재배정하면 409 CONFLICT")
    void assignStoreOrder_sameWarehouse() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);

        assertError(() -> storeOrderService.assignStoreOrder(
                new com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand(1L, 3L, "사유", 2L)),
                "CONFLICT");
        verify(storeOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("재고 할당이나 출고가 남아 있으면 재배정할 수 없다 (409 ORDER_IN_FULFILLMENT)")
    void assignStoreOrder_orderInFulfillment() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);
        when(storeOrderOutboundPort.existsActiveFulfillment(1L)).thenReturn(true);

        assertError(() -> storeOrderService.assignStoreOrder(
                new com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand(1L, 4L, "사유", 2L)),
                "ORDER_IN_FULFILLMENT");
        verify(storeOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("APPROVED·ASSIGNED가 아닌 발주(ON_HOLD 포함)는 배정할 수 없다 (409 CONFLICT)")
    void assignStoreOrder_invalidStatus() {
        for (StoreOrderStatus status : List.of(StoreOrderStatus.REQUESTED, StoreOrderStatus.ON_HOLD,
                StoreOrderStatus.COMPLETED, StoreOrderStatus.CANCELED, StoreOrderStatus.REJECTED)) {
            when(storeOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(orderIn(1L, status)));

            assertError(() -> storeOrderService.assignStoreOrder(
                    new com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand(1L, 4L, "사유", 2L)),
                    "CONFLICT");
        }
        verifyNoInteractions(warehouseAvailabilityPort, statusHistoryUseCase);
    }

    @Test
    @DisplayName("창고가 없거나 비활성이면 창고 포트의 예외가 전파되고 저장하지 않는다")
    void assignStoreOrder_warehouseNotAvailable() {
        givenLockedOrder(1L, StoreOrderStatus.APPROVED);
        doThrow(new BusinessException(com.kb.wms.common.exception.ErrorCode.CONFLICT, "비활성 창고"))
                .when(warehouseAvailabilityPort).requireActive(3L);

        assertError(() -> storeOrderService.assignStoreOrder(
                new com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand(1L, 3L, null, 2L)),
                "CONFLICT");
        verify(storeOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("없는 발주를 배정하면 404, 발주·창고 ID가 없으면 400")
    void assignStoreOrder_notFoundAndValidation() {
        when(storeOrderRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());
        assertError(() -> storeOrderService.assignStoreOrder(
                new com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand(9L, 3L, null, 2L)),
                "STORE_ORDER_NOT_FOUND");
        assertError(() -> storeOrderService.assignStoreOrder(
                new com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand(null, 3L, null, 2L)),
                "VALIDATION_ERROR");
        assertError(() -> storeOrderService.assignStoreOrder(
                new com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand(1L, null, null, 2L)),
                "VALIDATION_ERROR");
    }

    // ---------- holdStoreOrder ----------

    @Test
    @DisplayName("ASSIGNED 발주를 보류하면 ON_HOLD가 되고 사유를 이력과 응답에 남긴다")
    void holdStoreOrder_success() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);
        when(storeOrderOutboundPort.existsPickingStarted(1L)).thenReturn(false);

        StoreOrderStatusChange result = storeOrderService.holdStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderHoldCommand(1L, "재고 부족", 7L), HQ);

        assertThat(result.status()).isEqualTo(StoreOrderStatus.ON_HOLD);
        assertThat(result.statusReason()).isEqualTo("재고 부족");
        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.STORE_ORDER, 1L, "ASSIGNED", "ON_HOLD", "재고 부족", 7L);
    }

    @Test
    @DisplayName("보류 사유가 없으면 400, ASSIGNED가 아니면 409 CONFLICT, 피킹이 시작됐으면 409 ORDER_IN_PICKING")
    void holdStoreOrder_failures() {
        assertError(() -> storeOrderService.holdStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderHoldCommand(1L, null, 7L), HQ),
                "VALIDATION_ERROR");

        when(storeOrderRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(orderIn(1L, StoreOrderStatus.ON_HOLD)));
        assertError(() -> storeOrderService.holdStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderHoldCommand(1L, "사유", 7L), HQ),
                "CONFLICT");

        when(storeOrderRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(orderIn(2L, StoreOrderStatus.ASSIGNED)));
        when(storeOrderOutboundPort.existsPickingStarted(2L)).thenReturn(true);
        assertError(() -> storeOrderService.holdStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderHoldCommand(2L, "사유", 7L), HQ),
                "ORDER_IN_PICKING");

        verify(storeOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    // ---------- resumeStoreOrder ----------

    @Test
    @DisplayName("ON_HOLD 발주를 재개하면 ASSIGNED가 되고 statusReason은 null이다")
    void resumeStoreOrder_success() {
        givenLockedOrder(1L, StoreOrderStatus.ON_HOLD);

        StoreOrderStatusChange result = storeOrderService.resumeStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderResumeCommand(1L, "재고 입고", 7L), HQ);

        assertThat(result.status()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(result.statusReason()).isNull();
        verify(warehouseAvailabilityPort).requireActive(3L);
        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.STORE_ORDER, 1L, "ON_HOLD", "ASSIGNED", "재고 입고", 7L);
    }

    @Test
    @DisplayName("재개 사유가 없으면 400, ON_HOLD가 아니면(보류 중 취소 포함) 409 CONFLICT")
    void resumeStoreOrder_failures() {
        assertError(() -> storeOrderService.resumeStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderResumeCommand(1L, " ", 7L), HQ),
                "VALIDATION_ERROR");

        for (StoreOrderStatus status : List.of(StoreOrderStatus.ASSIGNED, StoreOrderStatus.CANCELED)) {
            when(storeOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(orderIn(1L, status)));
            assertError(() -> storeOrderService.resumeStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderResumeCommand(1L, "사유", 7L), HQ),
                    "CONFLICT");
        }
        verifyNoInteractions(warehouseAvailabilityPort, statusHistoryUseCase);
    }

    @Test
    @DisplayName("배정 창고가 비활성이면 재개할 수 없다")
    void resumeStoreOrder_inactiveWarehouse() {
        givenLockedOrder(1L, StoreOrderStatus.ON_HOLD);
        doThrow(new BusinessException(com.kb.wms.common.exception.ErrorCode.CONFLICT, "비활성 창고"))
                .when(warehouseAvailabilityPort).requireActive(3L);

        assertError(() -> storeOrderService.resumeStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderResumeCommand(1L, "사유", 7L), HQ),
                "CONFLICT");
        verify(storeOrderRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    // ---------- completePartialStoreOrder ----------

    @Test
    @DisplayName("부족한 항목이 있으면 COMPLETED로 종결하고 항목 상태는 건드리지 않은 채 부족 수량을 계산해 돌려준다")
    void completePartialStoreOrder_success() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);
        givenLinesUnlocked(1L, shippedLine(1L, 10L, 10, 4), shippedLine(1L, 11L, 5, 5));
        when(storeOrderOutboundPort.existsInProgressOutbound(1L)).thenReturn(false);
        when(storeOrderOutboundPort.releaseUnlinkedAllocations(1L, 7L)).thenReturn(2);
        when(storeOrderQueryRepository.findLineViews(1L)).thenReturn(List.of(
                new StoreOrderLineView(100L, 10L, "SKU-10", "러닝화", "EA", 10L, 0L, 4L,
                        new BigDecimal("1500.00"), StoreOrderLineStatus.PARTIALLY_SHIPPED),
                new StoreOrderLineView(101L, 11L, "SKU-11", "양말", "EA", 5L, 0L, 5L,
                        new BigDecimal("200.00"), StoreOrderLineStatus.COMPLETED)));

        var result = storeOrderService.completePartialStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand(
                        1L, "공급 중단", 7L), HQ);

        assertThat(result.status()).isEqualTo(StoreOrderStatus.COMPLETED);
        assertThat(result.statusReason()).isEqualTo("공급 중단");
        assertThat(result.releasedAllocationCount()).isEqualTo(2);
        assertThat(result.items()).extracting(
                        com.kb.wms.storeorder.application.port.in.result.StoreOrderCompletePartialResult.Item::skuCode,
                        com.kb.wms.storeorder.application.port.in.result.StoreOrderCompletePartialResult.Item::shortageQuantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("SKU-10", 6L),
                        org.assertj.core.groups.Tuple.tuple("SKU-11", 0L));
        verify(storeOrderRepository, never()).saveLines(any());
        verify(storeOrderOutboundPort).releaseUnlinkedAllocations(1L, 7L);
        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.STORE_ORDER, 1L, "ASSIGNED", "COMPLETED", "공급 중단", 7L);
    }

    @Test
    @DisplayName("해제할 남은 할당이 없으면 releasedAllocationCount는 0이다")
    void completePartialStoreOrder_noLeftovers() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);
        givenLinesUnlocked(1L, shippedLine(1L, 10L, 10, 4));
        when(storeOrderOutboundPort.existsInProgressOutbound(1L)).thenReturn(false);
        when(storeOrderOutboundPort.releaseUnlinkedAllocations(1L, 7L)).thenReturn(0);
        when(storeOrderQueryRepository.findLineViews(1L)).thenReturn(List.of(
                new StoreOrderLineView(100L, 10L, "SKU-10", "러닝화", "EA", 10L, 0L, 4L,
                        new BigDecimal("1500.00"), StoreOrderLineStatus.PARTIALLY_SHIPPED)));

        var result = storeOrderService.completePartialStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand(
                        1L, "공급 중단", 7L), HQ);

        assertThat(result.releasedAllocationCount()).isZero();
        assertThat(result.status()).isEqualTo(StoreOrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("진행 중 출고가 있으면 409 OUTBOUND_IN_PROGRESS, 모두 출고됐으면 409 NO_SHORTAGE")
    void completePartialStoreOrder_guards() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);
        when(storeOrderOutboundPort.existsInProgressOutbound(1L)).thenReturn(true);
        assertError(() -> storeOrderService.completePartialStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand(
                        1L, "사유", 7L), HQ), "OUTBOUND_IN_PROGRESS");

        when(storeOrderOutboundPort.existsInProgressOutbound(1L)).thenReturn(false);
        givenLinesUnlocked(1L, shippedLine(1L, 10L, 10, 10), shippedLine(1L, 11L, 5, 6));
        assertError(() -> storeOrderService.completePartialStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand(
                        1L, "사유", 7L), HQ), "NO_SHORTAGE");

        verify(storeOrderRepository, never()).save(any());
        verify(storeOrderOutboundPort, never()).releaseUnlinkedAllocations(any(), any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("종결 사유가 없으면 400, ASSIGNED가 아니면 409 CONFLICT, 없는 발주는 404")
    void completePartialStoreOrder_otherFailures() {
        assertError(() -> storeOrderService.completePartialStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand(
                        1L, null, 7L), HQ), "VALIDATION_ERROR");

        when(storeOrderRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(orderIn(1L, StoreOrderStatus.ON_HOLD)));
        assertError(() -> storeOrderService.completePartialStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand(
                        1L, "사유", 7L), HQ), "CONFLICT");

        when(storeOrderRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());
        assertError(() -> storeOrderService.completePartialStoreOrder(new com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand(
                        9L, "사유", 7L), HQ), "STORE_ORDER_NOT_FOUND");
        verifyNoInteractions(statusHistoryUseCase);
    }

    private static AuthenticatedUser owner(long userId, long storeId) {
        return new AuthenticatedUser(userId, UserRole.STORE_OWNER, List.of(), List.of(storeId));
    }

    private static AuthenticatedUser manager(long userId, long warehouseId) {
        return new AuthenticatedUser(userId, UserRole.WAREHOUSE_MANAGER, List.of(warehouseId), List.of());
    }

    private static StoreOrderView assignedView(Long id) {
        return new StoreOrderView(id, "SO-20261004-0001", 1L, "강남점", 3L, "서울 물류센터", StoreOrderStatus.ASSIGNED,
                LocalDateTime.of(2026, 10, 4, 9, 0), null, null, 1L, new BigDecimal("1000.00"),
                0L, 5L, null, LocalDateTime.of(2026, 10, 4, 9, 0), LocalDateTime.of(2026, 10, 4, 9, 0));
    }

    @Test
    @DisplayName("발주 등록은 담당 지점의 점주만 할 수 있다")
    void register_otherStore_forbidden() {
        assertError(() -> storeOrderService.registerStoreOrder(registerCommand(null, null, line(10L, 1)), owner(6L, 2L)),
                "FORBIDDEN");
        verify(storeOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("승인 전 발주는 작성자 점주만 취소할 수 있고, 다른 점주·본사는 403이다")
    void cancel_requested_onlyAuthor() {
        givenLockedOrder(1L, StoreOrderStatus.REQUESTED);

        assertError(() -> storeOrderService.cancelStoreOrder(new StoreOrderCancelCommand(1L, null, 6L), owner(6L, 1L)),
                "FORBIDDEN");
        assertError(() -> storeOrderService.cancelStoreOrder(new StoreOrderCancelCommand(1L, null, 1L), HQ), "FORBIDDEN");
        verify(storeOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("승인 이후 발주는 본사만 취소할 수 있고, 작성자 점주도 403이다")
    void cancel_afterApproval_onlyHqAdmin() {
        givenLockedOrder(1L, StoreOrderStatus.APPROVED);

        assertError(() -> storeOrderService.cancelStoreOrder(new StoreOrderCancelCommand(1L, "사유", 5L), AUTHOR),
                "FORBIDDEN");
        verify(storeOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("단건·상세 조회는 점주는 담당 지점, 창고 관리자는 배정 창고의 발주만 볼 수 있다 (미배정은 불가)")
    void read_scope() {
        when(storeOrderQueryRepository.findView(1L)).thenReturn(Optional.of(assignedView(1L)));
        when(storeOrderQueryRepository.findView(2L)).thenReturn(Optional.of(view(2L, StoreOrderStatus.REQUESTED, 0)));

        assertThat(storeOrderService.getStoreOrder(1L, owner(5L, 1L)).view().storeOrderId()).isEqualTo(1L);
        assertThat(storeOrderService.getStoreOrder(1L, manager(7L, 3L)).view().storeOrderId()).isEqualTo(1L);
        assertThat(storeOrderService.getStoreOrder(1L, HQ).view().storeOrderId()).isEqualTo(1L);

        assertError(() -> storeOrderService.getStoreOrder(1L, owner(6L, 2L)), "FORBIDDEN");
        assertError(() -> storeOrderService.getStoreOrderDetails(1L, owner(6L, 2L)), "FORBIDDEN");
        assertError(() -> storeOrderService.getStoreOrder(1L, manager(8L, 9L)), "FORBIDDEN");
        assertError(() -> storeOrderService.getStoreOrderDetails(1L, manager(8L, 9L)), "FORBIDDEN");
        // 창고가 아직 배정되지 않은 발주는 창고 관리자에게 보이지 않는다
        assertError(() -> storeOrderService.getStoreOrder(2L, manager(7L, 3L)), "FORBIDDEN");
    }

    @Test
    @DisplayName("보류·재개·부분 종결은 배정 창고가 담당 창고가 아니면 403이고 상태를 바꾸지 않는다")
    void fulfillmentActions_otherWarehouse_forbidden() {
        givenLockedOrder(1L, StoreOrderStatus.ASSIGNED);
        AuthenticatedUser other = manager(8L, 9L);

        assertError(() -> storeOrderService.holdStoreOrder(new StoreOrderHoldCommand(1L, "사유", 8L), other), "FORBIDDEN");
        assertError(() -> storeOrderService.resumeStoreOrder(new StoreOrderResumeCommand(1L, "사유", 8L), other), "FORBIDDEN");
        assertError(() -> storeOrderService.completePartialStoreOrder(
                new StoreOrderCompletePartialCommand(1L, "사유", 8L), other), "FORBIDDEN");
        verify(storeOrderRepository, never()).save(any());
    }


    @Test
    @DisplayName("다른 지점 사용자의 취소는 발주 상태와 상관없이 403이다 (상태 409가 드러나지 않는다). 소속이 바뀐 작성자도 마찬가지다")
    void cancel_otherStore_forbiddenBeforeStateCheck() {
        // 작성자(5번)의 소속이 지점 2로 바뀌어 발주 지점(1)의 담당이 아닌 경우
        AuthenticatedUser movedAuthor = owner(5L, 2L);
        AuthenticatedUser otherOwner = owner(6L, 2L);

        for (StoreOrderStatus status : StoreOrderStatus.values()) {
            givenLockedOrder(1L, status);
            for (AuthenticatedUser actor : List.of(movedAuthor, otherOwner)) {
                assertError(() -> storeOrderService.cancelStoreOrder(
                        new StoreOrderCancelCommand(1L, "사유", actor.userId()), actor), "FORBIDDEN");
            }
        }
        verify(storeOrderRepository, never()).save(any());
        // 담당 지점의 본사 사용자에게는 종료된 발주가 그대로 409다
        givenLockedOrder(1L, StoreOrderStatus.COMPLETED);
        assertError(() -> storeOrderService.cancelStoreOrder(new StoreOrderCancelCommand(1L, "사유", 1L), HQ), "CONFLICT");
    }
}

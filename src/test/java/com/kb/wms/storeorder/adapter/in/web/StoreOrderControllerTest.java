package com.kb.wms.storeorder.adapter.in.web;

import static com.kb.wms.common.security.TestAuth.as;
import static com.kb.wms.common.security.TestAuth.signInAsHqAdmin;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.storeorder.application.port.in.StoreOrderUseCase;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCancelCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderHoldCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRegisterCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRejectCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderResumeCommand;
import com.kb.wms.storeorder.application.port.in.query.StoreOrderSearchCondition;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderAssignResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderCompletePartialResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetail;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetails;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderLineView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderListItem;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderOutboundView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderStatusChange;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderStatusHistoryView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderSummary;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderView;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderProgressStage;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.storeorder.exception.StoreOrderErrorCode;

@WebMvcTest(StoreOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class StoreOrderControllerTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 2, 12, 0);
    private static final LocalDateTime T1 = LocalDateTime.of(2026, 10, 2, 13, 0);

    private static final String REGISTER_BODY = """
            {
              "storeId": 2,
              "requestedDeliveryAt": "2030-10-05T09:00:00",
              "note": "weekly order",
              "lines": [
                { "skuId": 1, "requestedQuantity": 10 },
                { "skuId": 2, "requestedQuantity": 5 }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void signIn() {
        signInAsHqAdmin();
    }

    /** 처리 사용자 ID만 지정한 요청 주체. 서비스는 목이라 역할·범위 검사는 서비스 테스트에서 확인한다. */
    private static RequestPostProcessor user(long userId) {
        return as(UserRole.HQ_ADMIN, userId, java.util.List.of(), java.util.List.of());
    }

    @MockitoBean
    private StoreOrderUseCase storeOrderUseCase;

    // ---------- fixtures ----------

    private static StoreOrderView view(StoreOrderStatus status) {
        return new StoreOrderView(
                7L, "SO-20261002-0001", 2L, "Store A", 1L, "Warehouse 1", status,
                T0, LocalDateTime.of(2030, 10, 5, 9, 0), "weekly order", 2L,
                BigDecimal.valueOf(15000), 0L, 5L, "Kim", T0, T1);
    }

    private static List<StoreOrderLineView> lines() {
        return List.of(
                new StoreOrderLineView(70L, 1L, "SKU-1", "Ball", "EA", 10L, 0L, 4L,
                        BigDecimal.valueOf(1000), StoreOrderLineStatus.PARTIALLY_SHIPPED),
                new StoreOrderLineView(71L, 2L, "SKU-2", "Bat", "EA", 5L, 0L, 0L,
                        BigDecimal.valueOf(1000), StoreOrderLineStatus.REQUESTED));
    }

    private static StoreOrderStatusChange change(StoreOrderStatus status, String reason) {
        return new StoreOrderStatusChange(7L, "SO-20261002-0001", status, reason, T1);
    }

    private static BusinessException conflict() {
        return new BusinessException(ErrorCode.CONFLICT);
    }

    // ---------- 등록 ----------

    @Test
    @DisplayName("발주를 등록하면 201과 라인이 포함된 응답을 반환하고 userId가 createdBy로 전달된다")
    void register_success() throws Exception {
        when(storeOrderUseCase.registerStoreOrder(any(StoreOrderRegisterCommand.class), any())).thenReturn(7L);
        when(storeOrderUseCase.getStoreOrder(eq(7L), any()))
                .thenReturn(new StoreOrderDetail(view(StoreOrderStatus.REQUESTED), null,
                        StoreOrderProgressStage.PENDING_APPROVAL));
        when(storeOrderUseCase.getStoreOrderDetails(eq(7L), any()))
                .thenReturn(new StoreOrderDetails(7L, "SO-20261002-0001", StoreOrderStatus.REQUESTED,
                        StoreOrderProgressStage.PENDING_APPROVAL, lines(), List.of(), List.of()));

        mockMvc.perform(post("/api/v1/orders")
                        .with(user(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.storeOrderId").value(7))
                .andExpect(jsonPath("$.data.orderNo").value("SO-20261002-0001"))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.lines.length()").value(2))
                .andExpect(jsonPath("$.data.lines[0].skuCode").value("SKU-1"));

        ArgumentCaptor<StoreOrderRegisterCommand> captor = ArgumentCaptor.forClass(StoreOrderRegisterCommand.class);
        verify(storeOrderUseCase).registerStoreOrder(captor.capture(), any());
        StoreOrderRegisterCommand command = captor.getValue();
        assertThat(command.storeId()).isEqualTo(2L);
        assertThat(command.createdBy()).isEqualTo(5L);
        assertThat(command.lines()).hasSize(2);
        assertThat(command.lines().get(0).skuId()).isEqualTo(1L);
        assertThat(command.lines().get(0).requestedQuantity()).isEqualTo(10L);
    }

    @Test
    @DisplayName("라인이 비어 있으면 400 VALIDATION_ERROR")
    void register_emptyLines() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(user(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":2,\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(storeOrderUseCase);
    }

    @Test
    @DisplayName("수량이 1 미만이면 400 VALIDATION_ERROR")
    void register_quantityBelowOne() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(user(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":2,\"lines\":[{\"skuId\":1,\"requestedQuantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(storeOrderUseCase);
    }

    @Test
    @DisplayName("지점이 없으면 400 VALIDATION_ERROR")
    void register_missingStoreId() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(user(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"skuId\":1,\"requestedQuantity\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(storeOrderUseCase);
    }

    @Test
    @DisplayName("메모가 1000자를 넘으면 400 VALIDATION_ERROR")
    void register_noteTooLong() throws Exception {
        String body = "{\"storeId\":2,\"note\":\"" + "a".repeat(1001)
                + "\",\"lines\":[{\"skuId\":1,\"requestedQuantity\":1}]}";
        mockMvc.perform(post("/api/v1/orders")
                        .with(user(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(storeOrderUseCase);
    }

    @Test
    @DisplayName("공급 단가가 없으면 409 SUPPLY_PRICE_MISSING")
    void register_supplyPriceMissing() throws Exception {
        when(storeOrderUseCase.registerStoreOrder(any(StoreOrderRegisterCommand.class), any()))
                .thenThrow(new BusinessException(StoreOrderErrorCode.SUPPLY_PRICE_MISSING));

        mockMvc.perform(post("/api/v1/orders")
                        .with(user(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SUPPLY_PRICE_MISSING"));
    }

    // ---------- 목록 ----------

    @Test
    @DisplayName("목록 조회는 data.items로 반환하고 필터가 조건으로 전달된다")
    void list_success() throws Exception {
        StoreOrderSummary summary = new StoreOrderSummary(7L, "SO-20261002-0001", 2L, "Store A", 1L,
                "Warehouse 1", StoreOrderStatus.ASSIGNED, T0, null, 2L, BigDecimal.valueOf(15000), 1L);
        when(storeOrderUseCase.getStoreOrders(any(StoreOrderSearchCondition.class)))
                .thenReturn(List.of(new StoreOrderListItem(summary, StoreOrderOutboundStatus.PICKING,
                        StoreOrderProgressStage.PREPARING)));

        mockMvc.perform(get("/api/v1/orders")
                        .param("status", "ASSIGNED")
                        .param("storeId", "2")
                        .param("warehouseId", "1")
                        .param("keyword", "SO-2026")
                        .param("requestedFrom", "2026-10-01T00:00:00")
                        .param("requestedTo", "2026-10-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].orderNo").value("SO-20261002-0001"))
                .andExpect(jsonPath("$.data.items[0].status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.items[0].progressStage").value("PREPARING"))
                .andExpect(jsonPath("$.data.items[0].latestOutboundStatus").value("PICKING"));

        ArgumentCaptor<StoreOrderSearchCondition> captor = ArgumentCaptor.forClass(StoreOrderSearchCondition.class);
        verify(storeOrderUseCase).getStoreOrders(captor.capture());
        StoreOrderSearchCondition condition = captor.getValue();
        assertThat(condition.status()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(condition.storeId()).isEqualTo(2L);
        assertThat(condition.warehouseId()).isEqualTo(1L);
        assertThat(condition.keyword()).isEqualTo("SO-2026");
        assertThat(condition.requestedFrom()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(condition.requestedTo()).isEqualTo(LocalDateTime.of(2026, 10, 31, 23, 59, 59));
    }

    @Test
    @DisplayName("내 발주 목록은 필터와 토큰 사용자를 서비스에 넘기고 data.items로 반환하며, 서비스의 403을 그대로 응답한다")
    void myList_passesFilterAndPrincipal() throws Exception {
        StoreOrderSummary summary = new StoreOrderSummary(7L, "SO-20261002-0001", 2L, "Store A", 1L,
                "Warehouse 1", StoreOrderStatus.ASSIGNED, T0, null, 2L, BigDecimal.valueOf(15000), 0L);
        when(storeOrderUseCase.getMyStoreOrders(any(StoreOrderSearchCondition.class), any()))
                .thenReturn(List.of(new StoreOrderListItem(summary, null, StoreOrderProgressStage.PREPARING)));

        mockMvc.perform(get("/api/v1/orders/my").with(user(9L))
                        .param("status", "ASSIGNED").param("storeId", "2").param("keyword", "SO-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].orderNo").value("SO-20261002-0001"))
                .andExpect(jsonPath("$.data.items[0].progressStage").value("PREPARING"));

        ArgumentCaptor<StoreOrderSearchCondition> captor = ArgumentCaptor.forClass(StoreOrderSearchCondition.class);
        verify(storeOrderUseCase).getMyStoreOrders(captor.capture(), argThat(actor -> actor.userId().equals(9L)));
        assertThat(captor.getValue().status()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(captor.getValue().storeId()).isEqualTo(2L);
        assertThat(captor.getValue().keyword()).isEqualTo("SO-2026");

        when(storeOrderUseCase.getMyStoreOrders(any(StoreOrderSearchCondition.class), any()))
                .thenThrow(new BusinessException(com.kb.wms.common.exception.ErrorCode.FORBIDDEN));
        mockMvc.perform(get("/api/v1/orders/my").param("storeId", "99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("결과가 없으면 빈 items를 반환한다")
    void list_empty() throws Exception {
        when(storeOrderUseCase.getStoreOrders(any(StoreOrderSearchCondition.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    @DisplayName("잘못된 status 값이면 400 VALIDATION_ERROR")
    void list_invalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/orders").param("status", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(storeOrderUseCase);
    }

    @Test
    @DisplayName("잘못된 날짜 형식이면 400 VALIDATION_ERROR")
    void list_invalidDate() throws Exception {
        mockMvc.perform(get("/api/v1/orders").param("requestedFrom", "yesterday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(storeOrderUseCase);
    }

    // ---------- 단건 / 상세 ----------

    @Test
    @DisplayName("단건 조회는 헤더와 진행 단계를 반환한다")
    void get_success() throws Exception {
        when(storeOrderUseCase.getStoreOrder(eq(7L), any()))
                .thenReturn(new StoreOrderDetail(view(StoreOrderStatus.CANCELED), "changed mind",
                        StoreOrderProgressStage.CANCELED));

        mockMvc.perform(get("/api/v1/orders/{orderId}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeOrderId").value(7))
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.statusReason").value("changed mind"))
                .andExpect(jsonPath("$.data.progressStage").value("CANCELED"))
                .andExpect(jsonPath("$.data.createdByName").value("Kim"));
    }

    @Test
    @DisplayName("없는 발주를 조회하면 404 STORE_ORDER_NOT_FOUND")
    void get_notFound() throws Exception {
        when(storeOrderUseCase.getStoreOrder(eq(999L), any()))
                .thenThrow(new BusinessException(StoreOrderErrorCode.STORE_ORDER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/orders/{orderId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STORE_ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("orderId가 숫자가 아니면 400 VALIDATION_ERROR")
    void get_invalidId() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderId}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(storeOrderUseCase);
    }

    @Test
    @DisplayName("상세 조회는 라인, 출고, 상태 이력을 반환한다")
    void details_success() throws Exception {
        when(storeOrderUseCase.getStoreOrderDetails(eq(7L), any()))
                .thenReturn(new StoreOrderDetails(7L, "SO-20261002-0001", StoreOrderStatus.ASSIGNED,
                        StoreOrderProgressStage.PREPARING, lines(),
                        List.of(new StoreOrderOutboundView(100L, "OB-1", StoreOrderOutboundStatus.PICKING, null, null)),
                        List.of(new StoreOrderStatusHistoryView("REQUESTED", "APPROVED", null, 5L, "김본사", T1))));

        mockMvc.perform(get("/api/v1/orders/{orderId}/details", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeOrderId").value(7))
                .andExpect(jsonPath("$.data.progressStage").value("PREPARING"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].remainingQuantity").value(6))
                .andExpect(jsonPath("$.data.items[0].lineAmount").value(10000))
                .andExpect(jsonPath("$.data.outbounds[0].outboundNo").value("OB-1"))
                .andExpect(jsonPath("$.data.statusHistory[0].toStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.statusHistory[0].changedBy").value(5))
                .andExpect(jsonPath("$.data.statusHistory[0].changedByName").value("김본사"));
    }

    // ---------- 승인 ----------

    @Test
    @DisplayName("승인하면 APPROVED 상태를 반환하고 userId가 전달된다")
    void approve_success() throws Exception {
        when(storeOrderUseCase.approveStoreOrder(7L, 9L)).thenReturn(change(StoreOrderStatus.APPROVED, null));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/approve", 7L).with(user(9L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeOrderId").value(7))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        verify(storeOrderUseCase).approveStoreOrder(7L, 9L);
    }

    @Test
    @DisplayName("승인 대기 상태가 아니면 409 CONFLICT")
    void approve_conflict() throws Exception {
        when(storeOrderUseCase.approveStoreOrder(7L, 9L)).thenThrow(conflict());

        mockMvc.perform(patch("/api/v1/orders/{orderId}/approve", 7L).with(user(9L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    // ---------- 반려 ----------

    @Test
    @DisplayName("반려하면 사유가 응답에 포함되고 명령으로 전달된다")
    void reject_success() throws Exception {
        when(storeOrderUseCase.rejectStoreOrder(any(StoreOrderRejectCommand.class)))
                .thenReturn(change(StoreOrderStatus.CANCELED, "wrong items"));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/reject", 7L)
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"wrong items\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statusReason").value("wrong items"));

        ArgumentCaptor<StoreOrderRejectCommand> captor = ArgumentCaptor.forClass(StoreOrderRejectCommand.class);
        verify(storeOrderUseCase).rejectStoreOrder(captor.capture());
        assertThat(captor.getValue().storeOrderId()).isEqualTo(7L);
        assertThat(captor.getValue().reason()).isEqualTo("wrong items");
        assertThat(captor.getValue().changedBy()).isEqualTo(9L);
    }

    @Test
    @DisplayName("반려 사유가 비어 있거나 500자를 넘으면 400")
    void reject_invalidReason() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/{orderId}/reject", 7L)
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/reject", 7L)
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(storeOrderUseCase);
    }

    // ---------- 취소 ----------

    @Test
    @DisplayName("본문 없이 취소해도 되고 결과에 해제/취소 건수가 포함된다")
    void cancel_withoutBody() throws Exception {
        when(storeOrderUseCase.cancelStoreOrder(any(StoreOrderCancelCommand.class), any()))
                .thenReturn(new StoreOrderCancelResult(7L, "SO-20261002-0001", StoreOrderStatus.CANCELED,
                        null, 2, 1, T1));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/cancel", 7L).with(user(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.releasedAllocationCount").value(2))
                .andExpect(jsonPath("$.data.canceledOutboundCount").value(1));

        ArgumentCaptor<StoreOrderCancelCommand> captor = ArgumentCaptor.forClass(StoreOrderCancelCommand.class);
        verify(storeOrderUseCase).cancelStoreOrder(captor.capture(), any());
        assertThat(captor.getValue().reason()).isNull();
        assertThat(captor.getValue().changedBy()).isEqualTo(5L);
    }

    @Test
    @DisplayName("사유와 함께 취소하면 사유가 전달된다")
    void cancel_withReason() throws Exception {
        when(storeOrderUseCase.cancelStoreOrder(any(StoreOrderCancelCommand.class), any()))
                .thenReturn(new StoreOrderCancelResult(7L, "SO-20261002-0001", StoreOrderStatus.CANCELED,
                        "no longer needed", 0, 0, T1));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/cancel", 7L)
                        .with(user(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"no longer needed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statusReason").value("no longer needed"));

        ArgumentCaptor<StoreOrderCancelCommand> captor = ArgumentCaptor.forClass(StoreOrderCancelCommand.class);
        verify(storeOrderUseCase).cancelStoreOrder(captor.capture(), any());
        assertThat(captor.getValue().reason()).isEqualTo("no longer needed");
    }

    @Test
    @DisplayName("피킹 중이면 409 ORDER_IN_PICKING")
    void cancel_inPicking() throws Exception {
        when(storeOrderUseCase.cancelStoreOrder(any(StoreOrderCancelCommand.class), any()))
                .thenThrow(new BusinessException(StoreOrderErrorCode.ORDER_IN_PICKING));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/cancel", 7L).with(user(5L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_IN_PICKING"));
    }

    @Test
    @DisplayName("출고가 진행되었으면 409 ORDER_IN_FULFILLMENT")
    void cancel_inFulfillment() throws Exception {
        when(storeOrderUseCase.cancelStoreOrder(any(StoreOrderCancelCommand.class), any()))
                .thenThrow(new BusinessException(StoreOrderErrorCode.ORDER_IN_FULFILLMENT));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/cancel", 7L).with(user(5L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_IN_FULFILLMENT"));
    }

    @Test
    @DisplayName("취소 사유가 500자를 넘으면 400")
    void cancel_reasonTooLong() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/{orderId}/cancel", 7L)
                        .with(user(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(storeOrderUseCase);
    }

    // ---------- 배정 ----------

    @Test
    @DisplayName("배정하면 창고 정보가 포함된 응답을 반환하고 명령으로 전달된다")
    void assign_success() throws Exception {
        when(storeOrderUseCase.assignStoreOrder(any(StoreOrderAssignCommand.class)))
                .thenReturn(new StoreOrderAssignResult(7L, "SO-20261002-0001", StoreOrderStatus.ASSIGNED,
                        3L, "Warehouse 3", T1));

        mockMvc.perform(post("/api/v1/orders/assign")
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeOrderId\":7,\"warehouseId\":3,\"reason\":\"closer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.warehouseId").value(3))
                .andExpect(jsonPath("$.data.warehouseName").value("Warehouse 3"));

        ArgumentCaptor<StoreOrderAssignCommand> captor = ArgumentCaptor.forClass(StoreOrderAssignCommand.class);
        verify(storeOrderUseCase).assignStoreOrder(captor.capture());
        assertThat(captor.getValue().storeOrderId()).isEqualTo(7L);
        assertThat(captor.getValue().warehouseId()).isEqualTo(3L);
        assertThat(captor.getValue().reason()).isEqualTo("closer");
        assertThat(captor.getValue().changedBy()).isEqualTo(9L);
    }

    @Test
    @DisplayName("배정 시 warehouseId가 없으면 400")
    void assign_missingWarehouse() throws Exception {
        mockMvc.perform(post("/api/v1/orders/assign")
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeOrderId\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(storeOrderUseCase);
    }

    @Test
    @DisplayName("출고가 진행 중이면 409 OUTBOUND_IN_PROGRESS")
    void assign_outboundInProgress() throws Exception {
        when(storeOrderUseCase.assignStoreOrder(any(StoreOrderAssignCommand.class)))
                .thenThrow(new BusinessException(StoreOrderErrorCode.OUTBOUND_IN_PROGRESS));

        mockMvc.perform(post("/api/v1/orders/assign")
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeOrderId\":7,\"warehouseId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("OUTBOUND_IN_PROGRESS"));
    }

    // ---------- 보류 / 재개 / 부분 종결 ----------

    @Test
    @DisplayName("보류하면 ON_HOLD 상태와 사유를 반환한다")
    void hold_success() throws Exception {
        when(storeOrderUseCase.holdStoreOrder(any(StoreOrderHoldCommand.class), any()))
                .thenReturn(change(StoreOrderStatus.ON_HOLD, "out of stock"));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/hold", 7L)
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"out of stock\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_HOLD"))
                .andExpect(jsonPath("$.data.statusReason").value("out of stock"));

        ArgumentCaptor<StoreOrderHoldCommand> captor = ArgumentCaptor.forClass(StoreOrderHoldCommand.class);
        verify(storeOrderUseCase).holdStoreOrder(captor.capture(), any());
        assertThat(captor.getValue().reason()).isEqualTo("out of stock");
        assertThat(captor.getValue().changedBy()).isEqualTo(9L);
    }

    @Test
    @DisplayName("보류 사유가 없으면 400")
    void hold_missingReason() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/{orderId}/hold", 7L)
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(storeOrderUseCase);
    }

    @Test
    @DisplayName("재개하면 ASSIGNED 상태를 반환한다")
    void resume_success() throws Exception {
        when(storeOrderUseCase.resumeStoreOrder(any(StoreOrderResumeCommand.class), any()))
                .thenReturn(change(StoreOrderStatus.ASSIGNED, "restocked"));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/resume", 7L)
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"restocked\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));
    }

    @Test
    @DisplayName("보류 상태가 아닌 발주를 재개하면 409 CONFLICT")
    void resume_conflict() throws Exception {
        when(storeOrderUseCase.resumeStoreOrder(any(StoreOrderResumeCommand.class), any())).thenThrow(conflict());

        mockMvc.perform(patch("/api/v1/orders/{orderId}/resume", 7L)
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"restocked\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    @DisplayName("부분 종결하면 라인별 부족 수량을 반환한다")
    void completePartial_success() throws Exception {
        when(storeOrderUseCase.completePartialStoreOrder(any(StoreOrderCompletePartialCommand.class), any()))
                .thenReturn(new StoreOrderCompletePartialResult(7L, "SO-20261002-0001", StoreOrderStatus.COMPLETED,
                        "no more stock",
                        List.of(new StoreOrderCompletePartialResult.Item(70L, "SKU-1", 10L, 4L, 6L)), 2, T1));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/complete-partial", 7L)
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"no more stock\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.items[0].skuCode").value("SKU-1"))
                .andExpect(jsonPath("$.data.items[0].shortageQuantity").value(6))
                .andExpect(jsonPath("$.data.releasedAllocationCount").value(2));
    }

    @Test
    @DisplayName("부족 라인이 없으면 409 NO_SHORTAGE")
    void completePartial_noShortage() throws Exception {
        when(storeOrderUseCase.completePartialStoreOrder(any(StoreOrderCompletePartialCommand.class), any()))
                .thenThrow(new BusinessException(StoreOrderErrorCode.NO_SHORTAGE));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/complete-partial", 7L)
                        .with(user(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"no more stock\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NO_SHORTAGE"));
    }
}

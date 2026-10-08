package com.kb.wms.inbound.adapter.in.web;

import static com.kb.wms.common.security.TestAuth.signInAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.application.port.in.PurchaseOrderUseCase;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderCancelCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRegisterCommand;
import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderDetails;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;

@WebMvcTest(PurchaseOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class PurchaseOrderControllerTest {

    private static final String REGISTER_BODY = """
            {
              "warehouseId": 1,
              "supplierId": 3,
              "expectedAt": "2030-09-25T09:00:00",
              "note": "정기 보충 발주",
              "lines": [
                { "skuId": 1, "expectedQuantity": 100 },
                { "skuId": 2, "expectedQuantity": 10 }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void signIn() {
        // 처리 사용자는 토큰 주체다. 서비스는 목이라 담당 창고 범위는 서비스 테스트에서 확인한다.
        signInAs(UserRole.WAREHOUSE_MANAGER, 5L, java.util.List.of(1L), java.util.List.of());
    }

    @MockitoBean
    private PurchaseOrderUseCase purchaseOrderUseCase;

    // ---------- fixtures ----------

    private static PurchaseOrderView view(PurchaseOrderStatus status) {
        return new PurchaseOrderView(
                4L, "PO-20261002-0001", 1L, "서울 물류센터", 3L, "공급처 A", status,
                LocalDateTime.of(2030, 9, 25, 9, 0), "정기 보충 발주", 1L, BigDecimal.valueOf(6000000), 5L,
                "김작성", LocalDateTime.of(2026, 10, 2, 12, 0), LocalDateTime.of(2026, 10, 2, 13, 0));
    }

    private static PurchaseOrderLineView lineView() {
        return new PurchaseOrderLineView(
                11L, 1L, "SKU-0001-RED-G4", "배드민턴 라켓 A 빨강 G4", "EA", 100L, 60L, 40L,
                BigDecimal.valueOf(60000), BigDecimal.valueOf(6000000), PurchaseOrderLineStatus.PARTIALLY_RECEIVED);
    }

    private static PurchaseOrder purchaseOrder(PurchaseOrderStatus status) {
        return PurchaseOrder.builder()
                .purchaseOrderId(4L)
                .purchaseOrderNo("PO-20261002-0001")
                .warehouseId(1L)
                .supplierId(3L)
                .status(status)
                .createdBy(5L)
                .updatedAt(LocalDateTime.of(2026, 10, 2, 14, 0))
                .build();
    }

    // ---------- 등록 ----------

    @Test
    @DisplayName("발주를 등록하면 201과 명세의 등록 응답(헤더 + 항목)을 반환한다")
    void register_success() throws Exception {
        when(purchaseOrderUseCase.registerPurchaseOrder(any(PurchaseOrderRegisterCommand.class), any())).thenReturn(4L);
        when(purchaseOrderUseCase.getPurchaseOrder(eq(4L), any())).thenReturn(view(PurchaseOrderStatus.REQUESTED));
        when(purchaseOrderUseCase.getPurchaseOrderDetails(eq(4L), any())).thenReturn(new PurchaseOrderDetails(
                4L, "PO-20261002-0001", PurchaseOrderStatus.REQUESTED, List.of(lineView())));

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.purchaseOrderId").value(4))
                .andExpect(jsonPath("$.data.purchaseOrderNo").value("PO-20261002-0001"))
                .andExpect(jsonPath("$.data.warehouseName").value("서울 물류센터"))
                .andExpect(jsonPath("$.data.supplierName").value("공급처 A"))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.totalAmount").value(6000000.0))
                .andExpect(jsonPath("$.data.createdBy").value(5))
                .andExpect(jsonPath("$.data.lines[0].purchaseOrderLineId").value(11))
                .andExpect(jsonPath("$.data.lines[0].skuCode").value("SKU-0001-RED-G4"))
                .andExpect(jsonPath("$.data.lines[0].expectedQuantity").value(100))
                .andExpect(jsonPath("$.data.lines[0].orderedUnitPrice").value(60000.0))
                .andExpect(jsonPath("$.data.lines[0].lineAmount").value(6000000.0));
    }

    @Test
    @DisplayName("등록 요청의 값과 userId를 커맨드로 변환해 유스케이스에 전달한다")
    void register_passesCommand() throws Exception {
        when(purchaseOrderUseCase.registerPurchaseOrder(any(PurchaseOrderRegisterCommand.class), any())).thenReturn(4L);
        when(purchaseOrderUseCase.getPurchaseOrder(eq(4L), any())).thenReturn(view(PurchaseOrderStatus.REQUESTED));
        when(purchaseOrderUseCase.getPurchaseOrderDetails(eq(4L), any())).thenReturn(new PurchaseOrderDetails(
                4L, "PO-20261002-0001", PurchaseOrderStatus.REQUESTED, List.of()));

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated());

        ArgumentCaptor<PurchaseOrderRegisterCommand> captor =
                ArgumentCaptor.forClass(PurchaseOrderRegisterCommand.class);
        verify(purchaseOrderUseCase).registerPurchaseOrder(captor.capture(), any());
        PurchaseOrderRegisterCommand command = captor.getValue();
        assertThat(command.warehouseId()).isEqualTo(1L);
        assertThat(command.supplierId()).isEqualTo(3L);
        assertThat(command.expectedAt()).isEqualTo(LocalDateTime.of(2030, 9, 25, 9, 0));
        assertThat(command.note()).isEqualTo("정기 보충 발주");
        assertThat(command.createdBy()).isEqualTo(5L);
        assertThat(command.lines()).containsExactly(
                new PurchaseOrderRegisterCommand.Line(1L, 100L),
                new PurchaseOrderRegisterCommand.Line(2L, 10L));
    }

    @Test
    @DisplayName("창고·공급처가 없거나 발주 항목이 비어 있으면 400 VALIDATION_ERROR를 반환한다")
    void register_missingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "lines": [] }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(purchaseOrderUseCase);
    }

    @Test
    @DisplayName("발주 수량이 0 이하이거나 SKU ID가 없으면 400 VALIDATION_ERROR를 반환한다")
    void register_invalidLine() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "warehouseId": 1,
                                  "supplierId": 3,
                                  "lines": [ { "skuId": 1, "expectedQuantity": 0 }, { "expectedQuantity": 5 } ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(purchaseOrderUseCase);
    }

    @Test
    @DisplayName("비고가 1000자를 넘으면 400 VALIDATION_ERROR를 반환한다")
    void register_noteTooLong() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "warehouseId": 1,
                                  "supplierId": 3,
                                  "note": "%s",
                                  "lines": [ { "skuId": 1, "expectedQuantity": 1 } ]
                                }
                                """.formatted("가".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(purchaseOrderUseCase);
    }

    @Test
    @DisplayName("SKU에 매입 단가가 없으면 409 PURCHASE_PRICE_MISSING을 반환한다")
    void register_purchasePriceMissing() throws Exception {
        when(purchaseOrderUseCase.registerPurchaseOrder(any(PurchaseOrderRegisterCommand.class), any()))
                .thenThrow(new BusinessException(PurchaseOrderErrorCode.PURCHASE_PRICE_MISSING));

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PURCHASE_PRICE_MISSING"));
    }

    @Test
    @DisplayName("비활성 창고·공급처·SKU로 등록하면 409 CONFLICT를 반환한다")
    void register_inactiveTarget() throws Exception {
        when(purchaseOrderUseCase.registerPurchaseOrder(any(PurchaseOrderRegisterCommand.class), any()))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "비활성 공급처에는 발주를 등록할 수 없습니다."));

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    // ---------- 목록 ----------

    @Test
    @DisplayName("목록 조회는 200과 data.items 형식으로 발주 헤더 요약을 반환한다")
    void getPurchaseOrders_success() throws Exception {
        when(purchaseOrderUseCase.getPurchaseOrders(any(PurchaseOrderSearchCondition.class), any()))
                .thenReturn(List.of(new PurchaseOrderSummary(
                        4L, "PO-20261002-0001", 1L, "서울 물류센터", 3L, "공급처 A", PurchaseOrderStatus.REQUESTED,
                        null, 2L, BigDecimal.valueOf(6050000), 5L, "김작성",
                        LocalDateTime.of(2026, 10, 2, 12, 0))));

        mockMvc.perform(get("/api/v1/purchase-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].purchaseOrderId").value(4))
                .andExpect(jsonPath("$.data.items[0].purchaseOrderNo").value("PO-20261002-0001"))
                .andExpect(jsonPath("$.data.items[0].createdByName").value("김작성"))
                .andExpect(jsonPath("$.data.items[0].warehouseName").value("서울 물류센터"))
                .andExpect(jsonPath("$.data.items[0].supplierName").value("공급처 A"))
                .andExpect(jsonPath("$.data.items[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.items[0].lineCount").value(2))
                .andExpect(jsonPath("$.data.items[0].totalAmount").value(6050000.0))
                .andExpect(jsonPath("$.data.items[0].note").doesNotExist());
    }

    @Test
    @DisplayName("목록 조회의 쿼리 파라미터를 검색 조건으로 변환해 전달한다")
    void getPurchaseOrders_passesCondition() throws Exception {
        when(purchaseOrderUseCase.getPurchaseOrders(any(PurchaseOrderSearchCondition.class), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("status", "CONFIRMED")
                        .param("warehouseId", "1")
                        .param("supplierId", "3")
                        .param("keyword", "PO-2026")
                        .param("createdFrom", "2026-10-01T00:00:00")
                        .param("createdTo", "2026-10-02T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        ArgumentCaptor<PurchaseOrderSearchCondition> captor =
                ArgumentCaptor.forClass(PurchaseOrderSearchCondition.class);
        verify(purchaseOrderUseCase).getPurchaseOrders(captor.capture(), any());
        PurchaseOrderSearchCondition condition = captor.getValue();
        assertThat(condition.status()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        assertThat(condition.warehouseId()).isEqualTo(1L);
        assertThat(condition.supplierId()).isEqualTo(3L);
        assertThat(condition.keyword()).isEqualTo("PO-2026");
        assertThat(condition.createdFrom()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(condition.createdTo()).isEqualTo(LocalDateTime.of(2026, 10, 2, 23, 59, 59));
    }

    @Test
    @DisplayName("파라미터를 주지 않으면 모든 조건을 null로 전달한다")
    void getPurchaseOrders_noParams() throws Exception {
        when(purchaseOrderUseCase.getPurchaseOrders(any(PurchaseOrderSearchCondition.class), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/purchase-orders")).andExpect(status().isOk());

        verify(purchaseOrderUseCase).getPurchaseOrders(eq(new PurchaseOrderSearchCondition(null, null, null, null, null, null)), any());
    }

    @Test
    @DisplayName("status 값이 올바르지 않으면 400 VALIDATION_ERROR를 반환한다")
    void getPurchaseOrders_invalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/purchase-orders").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(purchaseOrderUseCase);
    }

    @Test
    @DisplayName("일시 형식이 올바르지 않으면 400 VALIDATION_ERROR를 반환한다")
    void getPurchaseOrders_invalidDate() throws Exception {
        mockMvc.perform(get("/api/v1/purchase-orders").param("createdFrom", "yesterday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(purchaseOrderUseCase);
    }

    @Test
    @DisplayName("등록 시작 일시가 종료 일시보다 늦으면 400 VALIDATION_ERROR를 반환한다")
    void getPurchaseOrders_invalidRange() throws Exception {
        when(purchaseOrderUseCase.getPurchaseOrders(any(PurchaseOrderSearchCondition.class), any()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "등록 시작 일시는 종료 일시보다 늦을 수 없습니다."));

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("createdFrom", "2026-10-02T00:00:00")
                        .param("createdTo", "2026-10-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    // ---------- 상세 ----------

    @Test
    @DisplayName("발주 헤더 조회는 200과 명세의 헤더 필드를 반환한다")
    void getPurchaseOrder_success() throws Exception {
        when(purchaseOrderUseCase.getPurchaseOrder(eq(4L), any())).thenReturn(view(PurchaseOrderStatus.CONFIRMED));

        mockMvc.perform(get("/api/v1/purchase-orders/{purchaseOrderId}", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purchaseOrderId").value(4))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.note").value("정기 보충 발주"))
                .andExpect(jsonPath("$.data.lineCount").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(6000000.0))
                .andExpect(jsonPath("$.data.createdBy").value(5))
                .andExpect(jsonPath("$.data.createdByName").value("김작성"))
                .andExpect(jsonPath("$.data.lines").doesNotExist());
    }

    @Test
    @DisplayName("없는 발주를 조회하면 404 PURCHASE_ORDER_NOT_FOUND를 반환한다")
    void getPurchaseOrder_notFound() throws Exception {
        when(purchaseOrderUseCase.getPurchaseOrder(eq(999L), any()))
                .thenThrow(new BusinessException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/purchase-orders/{purchaseOrderId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PURCHASE_ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("발주 ID가 숫자가 아니면 400 VALIDATION_ERROR를 반환한다")
    void getPurchaseOrder_invalidId() throws Exception {
        mockMvc.perform(get("/api/v1/purchase-orders/{purchaseOrderId}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(purchaseOrderUseCase);
    }

    @Test
    @DisplayName("발주 항목 상세는 200과 SKU별 수량·남은 수량을 반환한다")
    void getPurchaseOrderDetails_success() throws Exception {
        when(purchaseOrderUseCase.getPurchaseOrderDetails(eq(4L), any())).thenReturn(new PurchaseOrderDetails(
                4L, "PO-20261002-0001", PurchaseOrderStatus.CONFIRMED, List.of(lineView())));

        mockMvc.perform(get("/api/v1/purchase-orders/{purchaseOrderId}/details", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purchaseOrderId").value(4))
                .andExpect(jsonPath("$.data.purchaseOrderNo").value("PO-20261002-0001"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.items[0].skuId").value(1))
                .andExpect(jsonPath("$.data.items[0].skuCode").value("SKU-0001-RED-G4"))
                .andExpect(jsonPath("$.data.items[0].unit").value("EA"))
                .andExpect(jsonPath("$.data.items[0].expectedQuantity").value(100))
                .andExpect(jsonPath("$.data.items[0].receivedQuantity").value(60))
                .andExpect(jsonPath("$.data.items[0].remainingQuantity").value(40))
                .andExpect(jsonPath("$.data.items[0].status").value("PARTIALLY_RECEIVED"));
    }

    @Test
    @DisplayName("없는 발주의 항목 상세를 조회하면 404 PURCHASE_ORDER_NOT_FOUND를 반환한다")
    void getPurchaseOrderDetails_notFound() throws Exception {
        when(purchaseOrderUseCase.getPurchaseOrderDetails(eq(999L), any()))
                .thenThrow(new BusinessException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/purchase-orders/{purchaseOrderId}/details", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PURCHASE_ORDER_NOT_FOUND"));
    }

    // ---------- 확정 ----------

    @Test
    @DisplayName("발주를 확정하면 200과 CONFIRMED 상태 응답을 반환한다")
    void confirm_success() throws Exception {
        when(purchaseOrderUseCase.confirmPurchaseOrder(4L, 5L)).thenReturn(purchaseOrder(PurchaseOrderStatus.CONFIRMED));

        mockMvc.perform(patch("/api/v1/purchase-orders/{purchaseOrderId}/confirm", 4L)
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purchaseOrderId").value(4))
                .andExpect(jsonPath("$.data.purchaseOrderNo").value("PO-20261002-0001"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.warehouseId").doesNotExist());
    }

    @Test
    @DisplayName("확정 대기 상태가 아닌 발주를 확정하면 409 CONFLICT를 반환한다")
    void confirm_conflict() throws Exception {
        when(purchaseOrderUseCase.confirmPurchaseOrder(4L, 5L))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "확정 대기 상태의 발주만 확정할 수 있습니다."));

        mockMvc.perform(patch("/api/v1/purchase-orders/{purchaseOrderId}/confirm", 4L)
                        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    @DisplayName("공급처가 비활성이면 409 SUPPLIER_INACTIVE를 반환한다")
    void confirm_supplierInactive() throws Exception {
        when(purchaseOrderUseCase.confirmPurchaseOrder(4L, 5L))
                .thenThrow(new BusinessException(PurchaseOrderErrorCode.SUPPLIER_INACTIVE));

        mockMvc.perform(patch("/api/v1/purchase-orders/{purchaseOrderId}/confirm", 4L)
                        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SUPPLIER_INACTIVE"));
    }

    @Test
    @DisplayName("없는 발주를 확정하면 404 PURCHASE_ORDER_NOT_FOUND를 반환한다")
    void confirm_notFound() throws Exception {
        when(purchaseOrderUseCase.confirmPurchaseOrder(999L, 5L))
                .thenThrow(new BusinessException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/purchase-orders/{purchaseOrderId}/confirm", 999L)
                        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PURCHASE_ORDER_NOT_FOUND"));
    }

    // ---------- 취소 ----------

    @Test
    @DisplayName("사유와 함께 취소하면 200과 CANCELED 상태 응답을 반환하고 사유를 커맨드로 전달한다")
    void cancel_success() throws Exception {
        when(purchaseOrderUseCase.cancelPurchaseOrder(eq(4L), any(PurchaseOrderCancelCommand.class), any()))
                .thenReturn(purchaseOrder(PurchaseOrderStatus.CANCELED));
        when(purchaseOrderUseCase.getPurchaseOrder(eq(4L), any()))
                .thenReturn(view(PurchaseOrderStatus.CANCELED).withCancelReason("공급업체 재고 부족으로 납품 불가"));

        mockMvc.perform(patch("/api/v1/purchase-orders/{purchaseOrderId}/cancel", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "공급업체 재고 부족으로 납품 불가" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purchaseOrderId").value(4))
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.cancelReason").value("공급업체 재고 부족으로 납품 불가"));

        verify(purchaseOrderUseCase).cancelPurchaseOrder(eq(4L), eq(new PurchaseOrderCancelCommand("공급업체 재고 부족으로 납품 불가", 5L)), any());
    }

    @Test
    @DisplayName("요청 본문 없이 취소해도 사유 null로 유스케이스에 전달한다")
    void cancel_withoutBody() throws Exception {
        when(purchaseOrderUseCase.cancelPurchaseOrder(eq(4L), any(PurchaseOrderCancelCommand.class), any()))
                .thenReturn(purchaseOrder(PurchaseOrderStatus.CANCELED));
        when(purchaseOrderUseCase.getPurchaseOrder(eq(4L), any())).thenReturn(view(PurchaseOrderStatus.CANCELED));

        mockMvc.perform(patch("/api/v1/purchase-orders/{purchaseOrderId}/cancel", 4L)
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.cancelReason").doesNotExist());

        verify(purchaseOrderUseCase).cancelPurchaseOrder(eq(4L), eq(new PurchaseOrderCancelCommand(null, 5L)), any());
    }

    @Test
    @DisplayName("취소 사유가 500자를 넘으면 400 VALIDATION_ERROR를 반환한다")
    void cancel_reasonTooLong() throws Exception {
        mockMvc.perform(patch("/api/v1/purchase-orders/{purchaseOrderId}/cancel", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "%s" }
                                """.formatted("가".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(purchaseOrderUseCase);
    }

    @Test
    @DisplayName("확정된 발주를 사유 없이 취소하면 400 VALIDATION_ERROR를 반환한다")
    void cancel_confirmedWithoutReason() throws Exception {
        when(purchaseOrderUseCase.cancelPurchaseOrder(eq(4L), any(PurchaseOrderCancelCommand.class), any()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "확정된 발주를 취소할 때는 사유를 입력해야 합니다."));

        mockMvc.perform(patch("/api/v1/purchase-orders/{purchaseOrderId}/cancel", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("취소된 발주 헤더를 조회하면 cancelReason을 반환한다")
    void getPurchaseOrder_canceled_returnsCancelReason() throws Exception {
        when(purchaseOrderUseCase.getPurchaseOrder(eq(4L), any()))
                .thenReturn(view(PurchaseOrderStatus.CANCELED).withCancelReason("입고 전 취소"));

        mockMvc.perform(get("/api/v1/purchase-orders/{purchaseOrderId}", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.cancelReason").value("입고 전 취소"));
    }

    @Test
    @DisplayName("이미 취소·완료된 발주를 취소하면 409 CONFLICT를 반환한다")
    void cancel_conflict() throws Exception {
        when(purchaseOrderUseCase.cancelPurchaseOrder(eq(4L), any(PurchaseOrderCancelCommand.class), any()))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "요청 또는 확정 상태의 발주만 취소할 수 있습니다."));

        mockMvc.perform(patch("/api/v1/purchase-orders/{purchaseOrderId}/cancel", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "사유" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    @DisplayName("없는 발주를 취소하면 404 PURCHASE_ORDER_NOT_FOUND를 반환한다")
    void cancel_notFound() throws Exception {
        when(purchaseOrderUseCase.cancelPurchaseOrder(eq(999L), any(PurchaseOrderCancelCommand.class), any()))
                .thenThrow(new BusinessException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/purchase-orders/{purchaseOrderId}/cancel", 999L)
                        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PURCHASE_ORDER_NOT_FOUND"));
    }
}

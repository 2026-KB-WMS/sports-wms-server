package com.kb.wms.inbound.adapter.in.web;

import static com.kb.wms.common.security.TestAuth.signInAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.inbound.application.port.in.SupplierOrderUseCase;
import com.kb.wms.inbound.application.port.in.query.SupplierOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderDetails;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderLineView;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderView;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;
import com.kb.wms.inbound.exception.SupplierOrderErrorCode;

@WebMvcTest(SupplierOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class SupplierOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierOrderUseCase supplierOrderUseCase;

    @BeforeEach
    void signIn() {
        signInAs(UserRole.WAREHOUSE_MANAGER, 5L, List.of(1L), List.of());
    }

    @Test
    @DisplayName("목록은 200과 data.items 형식으로 공급처 발주서 요약을 반환한다")
    void getSupplierOrders_success() throws Exception {
        when(supplierOrderUseCase.getSupplierOrders(any(SupplierOrderSearchCondition.class), any()))
                .thenReturn(List.of(new SupplierOrderSummary(
                        21L, "SO-20261015-0001", 4L, "PO-20261015-0001", 3L, "공급처 A", 1L, "서울 물류센터",
                        SupplierOrderStatus.ISSUED, 2L, BigDecimal.valueOf(6050000),
                        LocalDateTime.of(2026, 10, 15, 9, 0))));

        mockMvc.perform(get("/api/v1/supplier-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].supplierOrderId").value(21))
                .andExpect(jsonPath("$.data.items[0].supplierOrderNo").value("SO-20261015-0001"))
                .andExpect(jsonPath("$.data.items[0].purchaseOrderNo").value("PO-20261015-0001"))
                .andExpect(jsonPath("$.data.items[0].supplierName").value("공급처 A"))
                .andExpect(jsonPath("$.data.items[0].warehouseName").value("서울 물류센터"))
                .andExpect(jsonPath("$.data.items[0].status").value("ISSUED"))
                .andExpect(jsonPath("$.data.items[0].lineCount").value(2))
                .andExpect(jsonPath("$.data.items[0].totalAmount").value(6050000.0));
    }

    @Test
    @DisplayName("쿼리 파라미터를 검색 조건으로 변환해 전달한다")
    void getSupplierOrders_passesCondition() throws Exception {
        when(supplierOrderUseCase.getSupplierOrders(any(SupplierOrderSearchCondition.class), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/supplier-orders")
                        .param("status", "PARTIALLY_RECEIVED")
                        .param("warehouseId", "1")
                        .param("supplierId", "3")
                        .param("purchaseOrderId", "4")
                        .param("keyword", "SO-2026")
                        .param("createdFrom", "2026-10-01T00:00:00")
                        .param("createdTo", "2026-10-02T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        ArgumentCaptor<SupplierOrderSearchCondition> captor =
                ArgumentCaptor.forClass(SupplierOrderSearchCondition.class);
        verify(supplierOrderUseCase).getSupplierOrders(captor.capture(), any());
        SupplierOrderSearchCondition condition = captor.getValue();
        assertThat(condition.status()).isEqualTo(SupplierOrderStatus.PARTIALLY_RECEIVED);
        assertThat(condition.warehouseId()).isEqualTo(1L);
        assertThat(condition.supplierId()).isEqualTo(3L);
        assertThat(condition.purchaseOrderId()).isEqualTo(4L);
        assertThat(condition.keyword()).isEqualTo("SO-2026");
        assertThat(condition.createdFrom()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
    }

    @Test
    @DisplayName("status 값이 올바르지 않으면 400 VALIDATION_ERROR를 반환한다")
    void getSupplierOrders_invalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/supplier-orders").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(supplierOrderUseCase);
    }

    @Test
    @DisplayName("상세는 200과 헤더·항목을 반환한다")
    void getSupplierOrder_success() throws Exception {
        SupplierOrderView header = new SupplierOrderView(
                21L, "SO-20261015-0001", 4L, "PO-20261015-0001", 3L, "공급처 A", 1L, "서울 물류센터",
                SupplierOrderStatus.PARTIALLY_RECEIVED, 1L, BigDecimal.valueOf(100000),
                LocalDateTime.of(2026, 10, 15, 9, 0), LocalDateTime.of(2026, 10, 15, 10, 0));
        SupplierOrderLineView line = new SupplierOrderLineView(
                11L, 1L, "SKU-0001", "배드민턴 라켓", "EA", 100L, 60L, 40L, BigDecimal.valueOf(1000),
                BigDecimal.valueOf(100000), PurchaseOrderLineStatus.PARTIALLY_RECEIVED);
        when(supplierOrderUseCase.getSupplierOrder(eq(21L), any()))
                .thenReturn(new SupplierOrderDetails(header, List.of(line)));

        mockMvc.perform(get("/api/v1/supplier-orders/{supplierOrderId}", 21L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierOrderId").value(21))
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_RECEIVED"))
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.lines[0].purchaseOrderLineId").value(11))
                .andExpect(jsonPath("$.data.lines[0].skuCode").value("SKU-0001"))
                .andExpect(jsonPath("$.data.lines[0].remainingQuantity").value(40))
                .andExpect(jsonPath("$.data.lines[0].status").value("PARTIALLY_RECEIVED"));
    }

    @Test
    @DisplayName("없는 발주서는 404 SUPPLIER_ORDER_NOT_FOUND를 반환한다")
    void getSupplierOrder_notFound() throws Exception {
        when(supplierOrderUseCase.getSupplierOrder(eq(999L), any()))
                .thenThrow(new BusinessException(SupplierOrderErrorCode.SUPPLIER_ORDER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/supplier-orders/{supplierOrderId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SUPPLIER_ORDER_NOT_FOUND"));
    }
}

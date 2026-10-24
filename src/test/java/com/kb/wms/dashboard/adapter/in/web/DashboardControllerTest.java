package com.kb.wms.dashboard.adapter.in.web;

import static com.kb.wms.common.security.TestAuth.hqAdmin;
import static com.kb.wms.common.security.TestAuth.storeOwner;
import static com.kb.wms.common.security.TestAuth.warehouseManager;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.common.config.SecurityConfig;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.security.AuthenticatedUserResolver;
import com.kb.wms.common.security.JwtProvider;
import com.kb.wms.common.security.RestSecurityExceptionHandler;
import com.kb.wms.dashboard.application.port.in.HqDashboardUseCase;
import com.kb.wms.dashboard.application.port.in.result.FlowCount;
import com.kb.wms.dashboard.application.port.in.result.FlowTotals;
import com.kb.wms.dashboard.application.port.in.result.HqDashboardSummary;
import com.kb.wms.dashboard.application.port.in.result.HqDashboardTrends;
import com.kb.wms.dashboard.application.port.in.result.InventoryTotals;
import com.kb.wms.dashboard.application.port.in.result.MonthlyFlow;
import com.kb.wms.dashboard.application.port.in.result.PendingCounts;
import com.kb.wms.dashboard.application.port.in.result.StoreOrderStats;
import com.kb.wms.dashboard.application.port.in.result.TopOutboundSku;
import com.kb.wms.dashboard.application.port.in.result.WarehouseDashboardRow;
import com.kb.wms.dashboard.application.port.in.result.WarehouseFlowQuantity;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

/** 본사 홈 대시보드 API의 역할 규칙(본사 전용)과 응답 JSON 구조. */
@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class DashboardControllerTest {

    private static final List<String> PATHS = List.of("/api/v1/dashboard/hq/summary", "/api/v1/dashboard/hq/trends");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HqDashboardUseCase hqDashboardUseCase;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Test
    @DisplayName("토큰이 없으면 401")
    void withoutToken_unauthorized() throws Exception {
        for (String path : PATHS) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("창고 관리자와 점주는 403")
    void nonHq_forbidden() throws Exception {
        for (String path : PATHS) {
            mockMvc.perform(get(path).with(warehouseManager(2L, 1L)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
            mockMvc.perform(get(path).with(storeOwner(3L, 1L)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        }
    }

    @Test
    @DisplayName("요약은 명세 JSON 구조로 응답한다")
    void summary_shape() throws Exception {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("REQUESTED", 4L);
        byStatus.put("COMPLETED", 51L);
        when(hqDashboardUseCase.getSummary(any())).thenReturn(new HqDashboardSummary(
                YearMonth.of(2026, 10),
                new PendingCounts(4L, 2L, 3L),
                new InventoryTotals(184200L, 1200L, 300L, 150L, 450L, 312L, 18L, 7L),
                new FlowTotals(new FlowCount(21L, 9800L), new FlowCount(3L, 1200L), new FlowCount(19L, 9100L)),
                new FlowTotals(new FlowCount(64L, 7300L), new FlowCount(9L, 950L), new FlowCount(58L, 6900L)),
                new StoreOrderStats(72L, byStatus, new BigDecimal("0.73")),
                List.of(new WarehouseDashboardRow(1L, "서울 센터", 98000L, 10L,
                        new WarehouseFlowQuantity(5200L, 600L), new WarehouseFlowQuantity(3900L, 450L)))));

        mockMvc.perform(get("/api/v1/dashboard/hq/summary").param("month", "2026-10").with(hqAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value("2026-10"))
                .andExpect(jsonPath("$.data.pending.storeOrderRequestedCount").value(4))
                .andExpect(jsonPath("$.data.pending.unassignedApprovedStoreOrderCount").value(3))
                .andExpect(jsonPath("$.data.inventory.totalQuantity").value(184200))
                .andExpect(jsonPath("$.data.inventory.quarantinedQuantity").value(1200))
                .andExpect(jsonPath("$.data.inventory.expiredQuantity").value(150))
                .andExpect(jsonPath("$.data.inventory.defectiveQuantity").value(450))
                .andExpect(jsonPath("$.data.inventory.expiringLotCount").value(7))
                .andExpect(jsonPath("$.data.inbound.completed.count").value(21))
                .andExpect(jsonPath("$.data.inbound.inProgress.quantity").value(1200))
                .andExpect(jsonPath("$.data.outbound.previousMonthCompleted.quantity").value(6900))
                .andExpect(jsonPath("$.data.storeOrders.byStatus.COMPLETED").value(51))
                .andExpect(jsonPath("$.data.storeOrders.completionRate").value(0.73))
                .andExpect(jsonPath("$.data.warehouses[0].warehouseName").value("서울 센터"))
                .andExpect(jsonPath("$.data.warehouses[0].outbound.inProgressQuantity").value(450));
    }

    @Test
    @DisplayName("추이는 data.items와 topOutboundSkus로 응답하고 지난 달 진행중 수량은 null이다")
    void trends_shape() throws Exception {
        when(hqDashboardUseCase.getTrends(any())).thenReturn(new HqDashboardTrends(
                List.of(new MonthlyFlow(YearMonth.of(2026, 9), 8800L, 6500L, null, null),
                        new MonthlyFlow(YearMonth.of(2026, 10), 9800L, 7300L, 1200L, 950L)),
                List.of(new TopOutboundSku(12L, "SKU-0012", "축구공 5호", 840L))));

        mockMvc.perform(get("/api/v1/dashboard/hq/trends").with(hqAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].month").value("2026-09"))
                .andExpect(jsonPath("$.data.items[0].inboundInProgressQuantity").isEmpty())
                .andExpect(jsonPath("$.data.items[1].outboundInProgressQuantity").value(950))
                .andExpect(jsonPath("$.data.topOutboundSkus[0].skuCode").value("SKU-0012"))
                .andExpect(jsonPath("$.data.topOutboundSkus[0].quantity").value(840));
    }

    @Test
    @DisplayName("month 형식이 틀리거나 숫자가 아니면 400 VALIDATION_ERROR")
    void invalidParams_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/hq/summary").param("month", "2026-13").with(hqAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/dashboard/hq/summary").param("month", "202610").with(hqAdmin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/dashboard/hq/trends").param("months", "abc").with(hqAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("없는 창고는 404 WAREHOUSE_NOT_FOUND")
    void trends_warehouseNotFound() throws Exception {
        when(hqDashboardUseCase.getTrends(any()))
                .thenThrow(new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/dashboard/hq/trends").param("warehouseId", "99").with(hqAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("WAREHOUSE_NOT_FOUND"));
    }
}

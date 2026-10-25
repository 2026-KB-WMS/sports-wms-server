package com.kb.wms.dashboard.adapter.in.web.warehouse;

import static com.kb.wms.common.security.TestAuth.hqAdmin;
import static com.kb.wms.common.security.TestAuth.storeOwner;
import static com.kb.wms.common.security.TestAuth.warehouseManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.common.config.SecurityConfig;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.AuthenticatedUserResolver;
import com.kb.wms.common.security.JwtProvider;
import com.kb.wms.common.security.RestSecurityExceptionHandler;
import com.kb.wms.dashboard.application.port.in.warehouse.WarehouseDashboardUseCase;
import com.kb.wms.dashboard.application.port.in.warehouse.query.WarehouseSummaryQuery;
import com.kb.wms.dashboard.application.port.in.warehouse.result.CapacityUsage;
import com.kb.wms.dashboard.application.port.in.warehouse.result.InboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.LowStockSku;
import com.kb.wms.dashboard.application.port.in.warehouse.result.OutboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseDashboardSummary;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseInventoryAlerts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseWorkQueue;

/** 창고 관리자 홈 대시보드 API의 역할 규칙(창고 관리자 전용)과 응답 JSON 구조. */
@WebMvcTest(WarehouseDashboardController.class)
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class WarehouseDashboardControllerTest {

    private static final String PATH = "/api/v1/dashboard/warehouse/summary";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarehouseDashboardUseCase warehouseDashboardUseCase;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Test
    @DisplayName("토큰이 없으면 401")
    void withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("본사 관리자와 점주는 403")
    void nonWarehouseManager_forbidden() throws Exception {
        mockMvc.perform(get(PATH).param("warehouseId", "1").with(hqAdmin()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        mockMvc.perform(get(PATH).with(storeOwner(3L, 1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("요약은 명세 JSON 구조로 응답하고 쿼리와 현재 사용자를 서비스에 넘긴다")
    void summary_shape() throws Exception {
        when(warehouseDashboardUseCase.getSummary(any(), any())).thenReturn(new WarehouseDashboardSummary(
                1L, "서울 센터",
                new WarehouseWorkQueue(new InboundWorkCounts(2L, 1L), new OutboundWorkCounts(5L, 2L, 3L)),
                CapacityUsage.of(BigDecimal.valueOf(10000), BigDecimal.valueOf(6200)),
                new WarehouseInventoryAlerts(18L,
                        List.of(new LowStockSku(12L, "SKU-0012", "축구공 5호", 3L, 20L)), 7L)));

        mockMvc.perform(get(PATH).param("warehouseId", "1").param("expiringWithinDays", "14")
                        .with(warehouseManager(2L, 1L, 4L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warehouseId").value(1))
                .andExpect(jsonPath("$.data.warehouseName").value("서울 센터"))
                .andExpect(jsonPath("$.data.pending.inbound.arrivedCount").value(2))
                .andExpect(jsonPath("$.data.pending.inbound.inspectingCount").value(1))
                .andExpect(jsonPath("$.data.pending.outbound.readyCount").value(5))
                .andExpect(jsonPath("$.data.pending.outbound.pickingCount").value(2))
                .andExpect(jsonPath("$.data.pending.outbound.shippedCount").value(3))
                .andExpect(jsonPath("$.data.capacity.capacity").value(10000))
                .andExpect(jsonPath("$.data.capacity.currentCapacity").value(6200))
                .andExpect(jsonPath("$.data.capacity.availableCapacity").value(3800))
                .andExpect(jsonPath("$.data.capacity.usageRate").value(0.62))
                .andExpect(jsonPath("$.data.inventory.lowStockSkuCount").value(18))
                .andExpect(jsonPath("$.data.inventory.lowStockSkus[0].skuCode").value("SKU-0012"))
                .andExpect(jsonPath("$.data.inventory.lowStockSkus[0].availableQuantity").value(3))
                .andExpect(jsonPath("$.data.inventory.lowStockSkus[0].safetyStockQuantity").value(20))
                .andExpect(jsonPath("$.data.inventory.expiringLotCount").value(7));

        ArgumentCaptor<AuthenticatedUser> principal = ArgumentCaptor.forClass(AuthenticatedUser.class);
        verify(warehouseDashboardUseCase).getSummary(eq(new WarehouseSummaryQuery(1L, 14)), principal.capture());
        assertThat(principal.getValue().warehouseIds()).containsExactly(1L, 4L);
    }

    @Test
    @DisplayName("수용량이 0이면 usageRate는 null")
    void summary_zeroCapacity() throws Exception {
        when(warehouseDashboardUseCase.getSummary(any(), any())).thenReturn(new WarehouseDashboardSummary(
                1L, "서울 센터",
                new WarehouseWorkQueue(new InboundWorkCounts(0L, 0L), new OutboundWorkCounts(0L, 0L, 0L)),
                CapacityUsage.of(BigDecimal.ZERO, BigDecimal.ZERO),
                new WarehouseInventoryAlerts(0L, List.of(), 0L)));

        mockMvc.perform(get(PATH).with(warehouseManager(2L, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity.usageRate").isEmpty())
                .andExpect(jsonPath("$.data.inventory.lowStockSkus").isEmpty());
    }

    @Test
    @DisplayName("숫자가 아닌 파라미터와 서비스의 검증 오류는 400 VALIDATION_ERROR")
    void invalidParams_badRequest() throws Exception {
        mockMvc.perform(get(PATH).param("warehouseId", "abc").with(warehouseManager(2L, 1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(get(PATH).param("expiringWithinDays", "abc").with(warehouseManager(2L, 1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        when(warehouseDashboardUseCase.getSummary(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "담당 창고가 여럿이면 warehouseId가 필요합니다."));
        mockMvc.perform(get(PATH).with(warehouseManager(2L, 1L, 4L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("담당이 아닌 창고는 403 FORBIDDEN")
    void notAssignedWarehouse_forbidden() throws Exception {
        when(warehouseDashboardUseCase.getSummary(any(), any())).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get(PATH).param("warehouseId", "99").with(warehouseManager(2L, 1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }
}

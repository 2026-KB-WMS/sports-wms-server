package com.kb.wms.inventory.adapter.in.web;

import static com.kb.wms.common.security.TestAuth.hqAdmin;
import static com.kb.wms.common.security.TestAuth.storeOwner;
import static com.kb.wms.common.security.TestAuth.warehouseManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.common.config.SecurityConfig;
import com.kb.wms.common.security.AuthenticatedUserResolver;
import com.kb.wms.common.security.JwtProvider;
import com.kb.wms.common.security.RestSecurityExceptionHandler;
import com.kb.wms.inventory.application.port.in.InventoryAdjustmentUseCase;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.LotUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;

/** 재고·로트 API의 역할 규칙(docs/api/authorization.md). 담당 창고 범위는 컨트롤러 테스트에서 확인한다. */
@WebMvcTest({InventoryController.class, LotController.class})
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class InventoryAuthorizationTest {

    private static final List<String> READ_PATHS = List.of("/api/v1/inventory", "/api/v1/inventory/by-lot",
            "/api/v1/inventory/low-stock", "/api/v1/inventory/transactions", "/api/v1/inventory/1",
            "/api/v1/inventory/1/transactions", "/api/v1/lots", "/api/v1/lots/1");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryQueryUseCase inventoryQueryUseCase;
    @MockitoBean
    private InventoryAdjustmentUseCase inventoryAdjustmentUseCase;
    @MockitoBean
    private LotUseCase lotUseCase;
    @MockitoBean
    private WarehouseUseCase warehouseUseCase;
    @MockitoBean
    private WarehouseSectionUseCase warehouseSectionUseCase;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Test
    @DisplayName("토큰이 없으면 401")
    void withoutToken_unauthorized() throws Exception {
        for (String path : READ_PATHS) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/inventory/adjustments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("점주는 재고·로트 API를 전부 호출할 수 없다")
    void storeOwner_forbidden() throws Exception {
        for (String path : READ_PATHS) {
            mockMvc.perform(get(path).with(storeOwner(3L, 1L)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        }
        mockMvc.perform(post("/api/v1/inventory/adjustments").with(storeOwner(3L, 1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("조회는 본사와 창고 관리자가 역할 검사를 통과한다")
    void read_hqAndWarehouseManager_passRoleCheck() throws Exception {
        for (String path : READ_PATHS) {
            mockMvc.perform(get(path).with(hqAdmin()))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
        }
        // 창고 관리자는 지정하지 않은 목록을 담당 창고로 좁혀 조회한다
        mockMvc.perform(get("/api/v1/inventory").with(warehouseManager(2L, 1L))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("재고 조정은 창고 관리자만 할 수 있고, 본사는 403이다")
    void adjust_warehouseManagerOnly() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/adjustments").with(hqAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/inventory/adjustments").with(warehouseManager(2L, 1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }
}

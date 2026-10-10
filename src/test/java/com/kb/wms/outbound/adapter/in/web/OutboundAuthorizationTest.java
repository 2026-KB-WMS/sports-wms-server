package com.kb.wms.outbound.adapter.in.web;

import static com.kb.wms.common.security.TestAuth.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.config.SecurityConfig;
import com.kb.wms.common.security.AuthenticatedUserResolver;
import com.kb.wms.common.security.JwtProvider;
import com.kb.wms.common.security.RestSecurityExceptionHandler;
import com.kb.wms.outbound.application.port.in.OutboundFulfillmentUseCase;
import com.kb.wms.outbound.application.port.in.OutboundUseCase;
import com.kb.wms.outbound.application.port.in.StockAllocationUseCase;

/** 출고·재고 할당 API의 역할 규칙(docs/api/authorization.md). 담당 창고 범위는 서비스 통합 테스트에서 확인한다. */
@WebMvcTest({AllocationController.class, OutboundController.class})
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class OutboundAuthorizationTest {

    private static final List<String> READ_PATHS = List.of("/api/v1/allocations", "/api/v1/allocations/1",
            "/api/v1/outbounds", "/api/v1/outbounds/1/details");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockAllocationUseCase stockAllocationUseCase;
    @MockitoBean
    private OutboundUseCase outboundUseCase;
    @MockitoBean
    private OutboundFulfillmentUseCase outboundFulfillmentUseCase;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private AuthenticatedUserResolver authenticatedUserResolver;

    private static RequestPostProcessor hq() {
        return as(UserRole.HQ_ADMIN, 1L, List.of(), List.of());
    }

    private static RequestPostProcessor manager() {
        return as(UserRole.WAREHOUSE_MANAGER, 2L, List.of(1L), List.of());
    }

    private static RequestPostProcessor owner() {
        return as(UserRole.STORE_OWNER, 3L, List.of(), List.of(1L));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request) {
        return request.contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    private void assertNotRejected(RequestPostProcessor who, MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(who))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @DisplayName("토큰이 없으면 401")
    void withoutToken_unauthorized() throws Exception {
        for (String path : READ_PATHS) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("조회는 본사와 창고 관리자만 할 수 있고, 점주는 403이다")
    void read_hqAndWarehouseManager() throws Exception {
        for (String path : READ_PATHS) {
            mockMvc.perform(get(path).with(owner()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
            assertNotRejected(hq(), get(path));
            assertNotRejected(manager(), get(path));
        }
    }

    @Test
    @DisplayName("할당 생성·해제와 출고 생성·피킹·배송·취소는 창고 관리자만 할 수 있고, 본사·점주는 403이다")
    void writes_warehouseManagerOnly() throws Exception {
        for (var request : List.of(
                post("/api/v1/allocations"),
                patch("/api/v1/allocations/1/release"),
                post("/api/v1/outbounds"),
                patch("/api/v1/outbounds/1/picking/start"),
                patch("/api/v1/outbounds/1/picking/complete"),
                patch("/api/v1/outbounds/1/ship"),
                patch("/api/v1/outbounds/1/deliver"),
                patch("/api/v1/outbounds/1/cancel"))) {
            mockMvc.perform(json(request).with(hq())).andExpect(status().isForbidden());
            mockMvc.perform(json(request).with(owner())).andExpect(status().isForbidden());
            assertNotRejected(manager(), json(request));
        }
    }
}

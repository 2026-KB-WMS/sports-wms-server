package com.kb.wms.inbound.adapter.in.web;

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
import com.kb.wms.common.security.JwtProvider;
import com.kb.wms.common.security.RestSecurityExceptionHandler;
import com.kb.wms.inbound.application.port.in.InboundCompleteUseCase;
import com.kb.wms.inbound.application.port.in.InboundInspectUseCase;
import com.kb.wms.inbound.application.port.in.InboundUseCase;
import com.kb.wms.inbound.application.port.in.PurchaseOrderUseCase;
import com.kb.wms.inbound.application.port.in.SupplierUseCase;

/** 입고 API의 역할 규칙(docs/api/authorization.md). 담당 창고 검사는 서비스 테스트에서 확인한다. */
@WebMvcTest({InboundController.class})
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class InboundAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InboundUseCase inboundUseCase;
    @MockitoBean
    private InboundInspectUseCase inboundInspectUseCase;
    @MockitoBean
    private InboundCompleteUseCase inboundCompleteUseCase;
    @MockitoBean
    private JwtProvider jwtProvider;

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
        for (String path : List.of("/api/v1/inbounds", "/api/v1/inbounds/1")) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("입고 조회는 본사와 창고 관리자, 점주는 403이다")
    void inbounds_read() throws Exception {
        for (String path : List.of("/api/v1/inbounds", "/api/v1/inbounds/1", "/api/v1/inbounds/1/details",
                "/api/v1/inbounds/1/assignable-sections", "/api/v1/inbounds/1/defect-sections")) {
            mockMvc.perform(get(path).with(owner())).andExpect(status().isForbidden());
            assertNotRejected(hq(), get(path));
            assertNotRejected(manager(), get(path));
        }
    }

    @Test
    @DisplayName("입고 등록·검수·완료·취소는 창고 관리자만 할 수 있고, 본사는 403이다")
    void inbounds_write_warehouseManagerOnly() throws Exception {
        for (var request : List.of(post("/api/v1/inbounds"), patch("/api/v1/inbounds/1/inspect"),
                patch("/api/v1/inbounds/1/complete"), patch("/api/v1/inbounds/1/cancel"))) {
            mockMvc.perform(json(request).with(hq())).andExpect(status().isForbidden());
            mockMvc.perform(json(request).with(owner())).andExpect(status().isForbidden());
            assertNotRejected(manager(), json(request));
        }
    }
}

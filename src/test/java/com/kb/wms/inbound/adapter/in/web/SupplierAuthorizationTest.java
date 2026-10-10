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
import com.kb.wms.common.security.AuthenticatedUserResolver;
import com.kb.wms.common.security.JwtProvider;
import com.kb.wms.common.security.RestSecurityExceptionHandler;
import com.kb.wms.inbound.application.port.in.InboundCompleteUseCase;
import com.kb.wms.inbound.application.port.in.InboundInspectUseCase;
import com.kb.wms.inbound.application.port.in.InboundUseCase;
import com.kb.wms.inbound.application.port.in.PurchaseOrderUseCase;
import com.kb.wms.inbound.application.port.in.SupplierUseCase;

/** 공급처 API의 역할 규칙(docs/api/authorization.md). 창고 관리자에게 활성 공급처만 보이는 규칙은 서비스 테스트에서 확인한다. */
@WebMvcTest({SupplierController.class})
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class SupplierAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierUseCase supplierUseCase;
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
        for (String path : List.of("/api/v1/suppliers", "/api/v1/suppliers/1")) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("공급처 조회는 본사와 창고 관리자, 등록·수정·비활성화는 본사만이다")
    void suppliers() throws Exception {
        for (String path : List.of("/api/v1/suppliers", "/api/v1/suppliers/1")) {
            mockMvc.perform(get(path).with(owner())).andExpect(status().isForbidden());
            assertNotRejected(hq(), get(path));
            assertNotRejected(manager(), get(path));
        }
        for (var request : List.of(post("/api/v1/suppliers"), patch("/api/v1/suppliers/1"),
                patch("/api/v1/suppliers/1/deactivate"))) {
            mockMvc.perform(json(request).with(manager())).andExpect(status().isForbidden());
            mockMvc.perform(json(request).with(owner())).andExpect(status().isForbidden());
            assertNotRejected(hq(), json(request));
        }
    }
}

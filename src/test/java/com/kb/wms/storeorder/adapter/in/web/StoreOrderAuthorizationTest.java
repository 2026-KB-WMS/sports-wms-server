package com.kb.wms.storeorder.adapter.in.web;

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
import com.kb.wms.storeorder.application.port.in.StoreOrderUseCase;

/** 지점 발주 API의 역할 규칙(docs/api/authorization.md). 담당 지점·창고 범위와 취소 권한은 서비스 테스트에서 확인한다. */
@WebMvcTest(StoreOrderController.class)
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class StoreOrderAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreOrderUseCase storeOrderUseCase;
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

    private void assertOnly(List<RequestPostProcessor> allowed, List<RequestPostProcessor> denied,
                            MockHttpServletRequestBuilder request) throws Exception {
        for (RequestPostProcessor who : allowed) {
            assertNotRejected(who, request);
        }
        for (RequestPostProcessor who : denied) {
            mockMvc.perform(request.with(who))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        }
    }

    @Test
    @DisplayName("토큰이 없으면 401")
    void withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/orders/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("발주 등록은 점주만 할 수 있다")
    void register_storeOwnerOnly() throws Exception {
        assertOnly(List.of(owner()), List.of(hq(), manager()), json(post("/api/v1/orders")));
    }

    @Test
    @DisplayName("전체 발주 목록은 본사만 조회할 수 있다")
    void list_hqOnly() throws Exception {
        assertOnly(List.of(hq()), List.of(owner(), manager()), get("/api/v1/orders"));
    }

    @Test
    @DisplayName("내 발주 목록은 점주와 창고 관리자만 호출할 수 있고, 본사는 403이다")
    void myList_storeOwnerAndWarehouseManager() throws Exception {
        assertOnly(List.of(owner(), manager()), List.of(hq()), get("/api/v1/orders/my"));
    }

    @Test
    @DisplayName("발주 단건·상세는 본사·점주·창고 관리자가 호출할 수 있다 (범위는 서비스가 검사)")
    void read_allRoles() throws Exception {
        for (String path : List.of("/api/v1/orders/1", "/api/v1/orders/1/details")) {
            assertOnly(List.of(hq(), owner(), manager()), List.of(), get(path));
        }
    }

    @Test
    @DisplayName("승인·반려·창고 배정은 본사만 할 수 있다")
    void approveRejectAssign_hqOnly() throws Exception {
        for (var request : List.of(patch("/api/v1/orders/1/approve"), patch("/api/v1/orders/1/reject"),
                post("/api/v1/orders/assign"))) {
            assertOnly(List.of(hq()), List.of(owner(), manager()), json(request));
        }
    }

    @Test
    @DisplayName("취소는 본사와 점주만 호출할 수 있고, 창고 관리자는 403이다 (작성자·상태별 권한은 서비스가 검사)")
    void cancel_hqAndStoreOwner() throws Exception {
        assertOnly(List.of(hq(), owner()), List.of(manager()), json(patch("/api/v1/orders/1/cancel")));
    }

    @Test
    @DisplayName("보류·재개·부분 종결은 창고 관리자만 할 수 있다")
    void holdResumeCompletePartial_warehouseManagerOnly() throws Exception {
        for (var request : List.of(patch("/api/v1/orders/1/hold"), patch("/api/v1/orders/1/resume"),
                patch("/api/v1/orders/1/complete-partial"))) {
            assertOnly(List.of(manager()), List.of(hq(), owner()), json(request));
        }
    }
}

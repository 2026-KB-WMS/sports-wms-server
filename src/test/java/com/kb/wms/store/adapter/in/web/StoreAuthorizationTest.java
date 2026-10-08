package com.kb.wms.store.adapter.in.web;

import static com.kb.wms.common.security.TestAuth.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.kb.wms.store.application.port.in.StoreCodeUseCase;
import com.kb.wms.store.application.port.in.StoreMemberUseCase;
import com.kb.wms.store.application.port.in.StoreUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;

/** 지점 API의 역할 규칙(docs/api/authorization.md). 담당 지점 범위 검사는 서비스 테스트에서 확인한다. */
@WebMvcTest({StoreController.class, StoreMemberController.class, StoreCodeController.class})
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class StoreAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreUseCase storeUseCase;
    @MockitoBean
    private StoreMemberUseCase storeMemberUseCase;
    @MockitoBean
    private StoreCodeUseCase storeCodeUseCase;
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

    private void assertNotRejected(RequestPostProcessor who, MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(who))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @DisplayName("토큰이 없으면 401")
    void withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/stores/management-types")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/stores/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("역할 코드 목록은 인증된 모든 역할이 조회할 수 있다")
    void codes_anyRole() throws Exception {
        for (RequestPostProcessor who : List.of(hq(), manager(), owner())) {
            mockMvc.perform(get("/api/v1/stores/management-types").with(who)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("전체 지점 목록과 담당자 배정 목록은 본사만 조회할 수 있다")
    void lists_hqOnly() throws Exception {
        for (String path : List.of("/api/v1/stores", "/api/v1/stores/managers")) {
            mockMvc.perform(get(path).with(owner())).andExpect(status().isForbidden());
            mockMvc.perform(get(path).with(manager())).andExpect(status().isForbidden());
            assertNotRejected(hq(), get(path));
        }
    }

    @Test
    @DisplayName("지점 단건은 본사와 점주만, 창고 관리자는 403")
    void detail_hqAndStoreOwner() throws Exception {
        mockMvc.perform(get("/api/v1/stores/1").with(manager()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        assertNotRejected(hq(), get("/api/v1/stores/1"));
        assertNotRejected(owner(), get("/api/v1/stores/1"));
    }

    @Test
    @DisplayName("내 소속 지점은 점주만 조회할 수 있다")
    void my_storeOwnerOnly() throws Exception {
        mockMvc.perform(get("/api/v1/stores/my").with(hq())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/stores/my").with(manager())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/stores/my").with(owner())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("등록·수정·비활성화·배정·회수는 본사만, 점주와 창고 관리자는 403")
    void writes_hqOnly() throws Exception {
        for (var request : List.of(
                post("/api/v1/stores"),
                patch("/api/v1/stores/1"),
                patch("/api/v1/stores/1/deactivate"),
                post("/api/v1/stores/assign"),
                delete("/api/v1/stores/managers/1"))) {
            mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("{}").with(owner()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("{}").with(manager()))
                    .andExpect(status().isForbidden());
            assertNotRejected(hq(), request.contentType(MediaType.APPLICATION_JSON).content("{}"));
        }
    }
}

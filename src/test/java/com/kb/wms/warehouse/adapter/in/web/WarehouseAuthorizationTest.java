package com.kb.wms.warehouse.adapter.in.web;

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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.config.SecurityConfig;
import com.kb.wms.common.security.AuthenticatedUserResolver;
import com.kb.wms.common.security.JwtProvider;
import com.kb.wms.common.security.RestSecurityExceptionHandler;
import com.kb.wms.warehouse.application.port.in.WarehouseCodeUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseMemberUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionCapacityUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseSectionUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;

/** 창고 API의 역할 규칙(docs/api/authorization.md). 담당 창고 범위 검사는 각 컨트롤러 테스트에서 확인한다. */
@WebMvcTest({WarehouseController.class, WarehouseSectionController.class, WarehouseMemberController.class,
        WarehouseCodeController.class})
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class WarehouseAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarehouseUseCase warehouseUseCase;
    @MockitoBean
    private WarehouseSectionUseCase warehouseSectionUseCase;
    @MockitoBean
    private WarehouseMemberUseCase warehouseMemberUseCase;
    @MockitoBean
    private WarehouseCodeUseCase warehouseCodeUseCase;
    @MockitoBean
    private WarehouseSectionCapacityUseCase warehouseSectionCapacityUseCase;
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

    private void assertNotRejected(RequestPostProcessor who, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request.with(who))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @DisplayName("토큰이 없으면 401")
    void withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/warehouses/management-types")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/warehouses/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("코드 목록은 인증된 모든 역할이 조회할 수 있다")
    void codes_anyRole() throws Exception {
        for (RequestPostProcessor who : List.of(hq(), manager(), owner())) {
            mockMvc.perform(get("/api/v1/warehouses/management-types").with(who)).andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/warehouses/section-types").with(who)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("전체 목록(창고·구역·관리자 배정)은 본사만 조회할 수 있다")
    void lists_hqOnly() throws Exception {
        for (String path : List.of("/api/v1/warehouses", "/api/v1/warehouses/sections", "/api/v1/warehouses/managers")) {
            mockMvc.perform(get(path).with(manager())).andExpect(status().isForbidden());
            mockMvc.perform(get(path).with(owner())).andExpect(status().isForbidden());
            assertNotRejected(hq(), get(path));
        }
    }

    @Test
    @DisplayName("창고·구역 단건과 창고별 구역 목록은 본사와 창고 관리자만, 점주는 403")
    void details_hqAndWarehouseManager() throws Exception {
        for (String path : List.of("/api/v1/warehouses/1", "/api/v1/warehouses/1/sections",
                "/api/v1/warehouses/sections/1")) {
            mockMvc.perform(get(path).with(owner()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
            assertNotRejected(hq(), get(path));
            assertNotRejected(manager(), get(path));
        }
    }

    @Test
    @DisplayName("내 소속 창고는 창고 관리자만 조회할 수 있다")
    void my_warehouseManagerOnly() throws Exception {
        mockMvc.perform(get("/api/v1/warehouses/my").with(hq())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/warehouses/my").with(owner())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/warehouses/my").with(manager())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("등록·수정·비활성화·삭제·배정·회수는 본사만, 창고 관리자는 403")
    void writes_hqOnly() throws Exception {
        for (var request : List.of(
                post("/api/v1/warehouses"),
                patch("/api/v1/warehouses/1"),
                patch("/api/v1/warehouses/1/deactivate"),
                post("/api/v1/warehouses/sections"),
                patch("/api/v1/warehouses/sections/1"),
                patch("/api/v1/warehouses/sections/1/deactivate"),
                delete("/api/v1/warehouses/sections/1"),
                post("/api/v1/warehouses/managers"),
                delete("/api/v1/warehouses/managers/1"))) {
            mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("{}").with(manager()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("{}").with(owner()))
                    .andExpect(status().isForbidden());
            assertNotRejected(hq(), request.contentType(MediaType.APPLICATION_JSON).content("{}"));
        }
    }
}

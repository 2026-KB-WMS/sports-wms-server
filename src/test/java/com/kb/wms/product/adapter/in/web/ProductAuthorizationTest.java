package com.kb.wms.product.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.config.SecurityConfig;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.JwtProvider;
import com.kb.wms.common.security.RestSecurityExceptionHandler;
import com.kb.wms.product.application.port.in.BrandQueryUseCase;
import com.kb.wms.product.application.port.in.CategoryUseCase;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;
import com.kb.wms.product.application.port.in.ProductUseCase;

/** 상품 API의 역할 규칙(docs/api/authorization.md): 조회는 인증된 모든 역할, 등록·수정은 HQ_ADMIN만. */
@WebMvcTest({BrandController.class, ProductSkuController.class})
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class ProductAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandQueryUseCase brandQueryUseCase;
    @MockitoBean
    private ProductSkuUseCase productSkuUseCase;
    @MockitoBean
    private ProductUseCase productUseCase;
    @MockitoBean
    private CategoryUseCase categoryUseCase;
    // SecurityConfig가 필터를 만들 때 필요하다. 요청에는 Authorization 헤더를 싣지 않아 파싱은 일어나지 않는다.
    @MockitoBean
    private JwtProvider jwtProvider;

    private RequestPostProcessor as(UserRole role) {
        AuthenticatedUser user = new AuthenticatedUser(1L, role, List.of(), List.of());
        return authentication(UsernamePasswordAuthenticationToken.authenticated(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    @Test
    @DisplayName("토큰 없이 조회하면 401")
    void get_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/products/brands"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("조회는 인증된 모든 역할이 할 수 있다")
    void get_anyAuthenticatedRole_ok() throws Exception {
        for (UserRole role : UserRole.values()) {
            mockMvc.perform(get("/api/v1/products/brands").with(as(role)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("창고 관리자·점주가 등록하거나 수정하면 403")
    void write_nonHqRole_forbidden() throws Exception {
        for (UserRole role : List.of(UserRole.WAREHOUSE_MANAGER, UserRole.STORE_OWNER)) {
            mockMvc.perform(post("/api/v1/products/brands").with(as(role))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
            mockMvc.perform(patch("/api/v1/products/skus/1/status").with(as(role))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("본사 관리자는 역할 검사를 통과한다 (본문 검증 오류 400은 컨트롤러까지 도달했다는 뜻)")
    void write_hqAdmin_passesSecurity() throws Exception {
        mockMvc.perform(post("/api/v1/products/brands").with(as(UserRole.HQ_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }
}

package com.kb.wms.inbound.adapter.in.web;

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

import com.kb.wms.common.config.SecurityConfig;
import com.kb.wms.common.security.AuthenticatedUserResolver;
import com.kb.wms.common.security.JwtProvider;
import com.kb.wms.common.security.RestSecurityExceptionHandler;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.inbound.application.port.in.PurchaseOrderUseCase;
import com.kb.wms.inbound.application.port.in.SkuSupplierUseCase;
import com.kb.wms.inbound.application.port.in.SupplierOrderUseCase;

/**
 * 공급처 발주서 조회, SKU↔공급처 매핑, 발주 반려의 역할 규칙(docs/api/authorization.md).
 * 담당 창고 범위 검사는 서비스 테스트에서 확인한다.
 */
@WebMvcTest({SupplierOrderController.class, SkuSupplierController.class, PurchaseOrderController.class})
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class SupplierOrderAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierOrderUseCase supplierOrderUseCase;
    @MockitoBean
    private SkuSupplierUseCase skuSupplierUseCase;
    @MockitoBean
    private PurchaseOrderUseCase purchaseOrderUseCase;
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

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void assertNotRejected(RequestPostProcessor who, MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(who))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @DisplayName("공급처 발주서 조회는 본사와 창고 관리자만 가능하고 점주는 403, 토큰이 없으면 401이다")
    void supplierOrders_read() throws Exception {
        mockMvc.perform(get("/api/v1/supplier-orders")).andExpect(status().isUnauthorized());
        for (String path : List.of("/api/v1/supplier-orders", "/api/v1/supplier-orders/1")) {
            mockMvc.perform(get(path).with(owner()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
            assertNotRejected(hq(), get(path));
            assertNotRejected(manager(), get(path));
        }
    }

    @Test
    @DisplayName("SKU 공급처 매핑 조회는 본사와 창고 관리자, 등록·삭제는 본사만 가능하고 점주는 조회도 403이다")
    void skuSuppliers() throws Exception {
        String path = "/api/v1/products/skus/10/suppliers";

        mockMvc.perform(get(path).with(owner())).andExpect(status().isForbidden());
        assertNotRejected(hq(), get(path));
        assertNotRejected(manager(), get(path));

        mockMvc.perform(json(post(path), "{\"supplierId\":3}").with(manager())).andExpect(status().isForbidden());
        mockMvc.perform(json(post(path), "{\"supplierId\":3}").with(owner())).andExpect(status().isForbidden());
        assertNotRejected(hq(), json(post(path), "{\"supplierId\":3}"));

        mockMvc.perform(delete(path + "/3").with(manager())).andExpect(status().isForbidden());
        mockMvc.perform(delete(path + "/3").with(owner())).andExpect(status().isForbidden());
        assertNotRejected(hq(), delete(path + "/3"));
    }

    @Test
    @DisplayName("발주 반려는 본사만 호출할 수 있다")
    void purchaseOrders_reject() throws Exception {
        String path = "/api/v1/purchase-orders/1/reject";
        String body = "{\"reason\":\"수량 과다\"}";

        mockMvc.perform(json(patch(path), body).with(manager())).andExpect(status().isForbidden());
        mockMvc.perform(json(patch(path), body).with(owner())).andExpect(status().isForbidden());
        assertNotRejected(hq(), json(patch(path), body));
    }
}

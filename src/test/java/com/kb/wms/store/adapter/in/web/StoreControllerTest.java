package com.kb.wms.store.adapter.in.web;

import static com.kb.wms.common.security.TestAuth.as;
import static com.kb.wms.common.security.TestAuth.signInAsHqAdmin;
import static com.kb.wms.common.security.TestAuth.storeOwner;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.store.application.port.in.StoreUseCase;
import com.kb.wms.store.application.port.in.command.StoreRegisterCommand;
import com.kb.wms.store.application.port.in.command.StoreUpdateCommand;
import com.kb.wms.store.application.port.in.query.StoreSearchCondition;
import com.kb.wms.store.application.port.in.result.StoreMembershipSummary;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.exception.StoreErrorCode;

@WebMvcTest(StoreController.class)
@AutoConfigureMockMvc(addFilters = false)
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void signIn() {
        signInAsHqAdmin();
    }

    private static RequestPostProcessor hqUser(Long userId) {
        return as(UserRole.HQ_ADMIN, userId, List.of(), List.of());
    }
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private StoreUseCase storeUseCase;

    private record TestRegisterRequest(String storeCode, String storeName, String address,
                                        String contactName, String contactNumber) {
    }

    private record TestUpdateRequest(String storeName, String address, String contactName,
                                      String contactNumber, String storeCode, Boolean isActive) {
    }

    @Test
    @DisplayName("지점 등록에 성공하면 201과 등록된 지점을 반환한다")
    void registerStore_success() throws Exception {
        Store store = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        when(storeUseCase.registerStore(any(StoreRegisterCommand.class))).thenReturn(store);

        mockMvc.perform(post("/api/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRegisterRequest(
                                "ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.storeCode").value("ST-GANGNAM"))
                .andExpect(jsonPath("$.data.storeName").value("강남점"));
    }

    @Test
    @DisplayName("지점 코드가 중복되면 409 DUPLICATE_STORE_CODE를 반환한다")
    void registerStore_duplicateCode() throws Exception {
        when(storeUseCase.registerStore(any(StoreRegisterCommand.class)))
                .thenThrow(new BusinessException(StoreErrorCode.DUPLICATE_STORE_CODE));

        mockMvc.perform(post("/api/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRegisterRequest(
                                "ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_STORE_CODE"));
    }

    @Test
    @DisplayName("지점 목록을 조회하면 200과 목록을 반환한다")
    void getStores_success() throws Exception {
        Store store = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        when(storeUseCase.getStores(new StoreSearchCondition("강남", true)))
                .thenReturn(List.of(store));

        mockMvc.perform(get("/api/v1/stores").param("keyword", "강남").param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].storeCode").value("ST-GANGNAM"));
    }

    @Test
    @DisplayName("내 소속 지점을 조회하면 200과 items 배열로 감싼 응답을 반환한다")
    void getMyStores_success() throws Exception {
        StoreMembershipSummary summary = new StoreMembershipSummary(
                1L, "ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234",
                true, 100L, "OWNER", null);
        when(storeUseCase.getMyStores(eq(10L))).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/stores/my").with(storeOwner(10L, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].storeCode").value("ST-GANGNAM"))
                .andExpect(jsonPath("$.data.items[0].memberRole").value("OWNER"));
    }

    @Test
    @DisplayName("배정된 지점이 없으면 200과 빈 items 배열을 반환한다")
    void getMyStores_empty() throws Exception {
        when(storeUseCase.getMyStores(eq(999L))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/stores/my").with(storeOwner(999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    @DisplayName("지점 단건 조회는 토큰 사용자를 서비스에 넘기고, 서비스의 403 FORBIDDEN을 그대로 응답한다")
    void getStore_passesPrincipal_andMapsForbidden() throws Exception {
        when(storeUseCase.getStore(eq(2L), any())).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/v1/stores/{storeId}", 2L).with(storeOwner(10L, 1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        verify(storeUseCase).getStore(eq(2L), argThat(user -> user.userId().equals(10L)));
    }

    @Test
    @DisplayName("존재하지 않는 지점을 조회하면 404 STORE_NOT_FOUND를 반환한다")
    void getStore_notFound() throws Exception {
        when(storeUseCase.getStore(eq(999L), any())).thenThrow(new BusinessException(StoreErrorCode.STORE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/stores/{storeId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STORE_NOT_FOUND"));
    }

    @Test
    @DisplayName("지점을 수정하면 200과 수정된 지점을 반환한다")
    void updateStore_success() throws Exception {
        Store updated = Store.register("ST-GANGNAM", "새 이름", "서울시 강남구", "김점주", "02-333-1234");
        when(storeUseCase.updateStore(eq(1L), any(StoreUpdateCommand.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/stores/{storeId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestUpdateRequest("새 이름", null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeName").value("새 이름"));
    }

    @Test
    @DisplayName("storeCode·isActive 필드를 함께 보내 수정을 요청하면 400 VALIDATION_ERROR를 반환한다")
    void updateStore_immutableFields_returnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/stores/{storeId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestUpdateRequest(null, null, null, null, "ST-999", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("지점을 비활성화하면 200과 비활성화된 지점을 반환한다")
    void deactivateStore_success() throws Exception {
        Store store = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        store.deactivate();
        when(storeUseCase.deactivateStore(1L, null, 10L)).thenReturn(store);

        mockMvc.perform(patch("/api/v1/stores/{storeId}/deactivate", 1L).with(hqUser(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    @DisplayName("비활성화 요청 바디의 reason이 유스케이스에 전달된다")
    void deactivateStore_withReason() throws Exception {
        Store store = Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");
        store.deactivate();
        when(storeUseCase.deactivateStore(1L, "폐점", 10L)).thenReturn(store);

        mockMvc.perform(patch("/api/v1/stores/{storeId}/deactivate", 1L)
                        .with(hqUser(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"폐점\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    @DisplayName("비활성화 사유가 500자를 넘으면 400 VALIDATION_ERROR를 반환한다")
    void deactivateStore_reasonTooLong() throws Exception {
        mockMvc.perform(patch("/api/v1/stores/{storeId}/deactivate", 1L)
                        .with(hqUser(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("reason", "가".repeat(501)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}

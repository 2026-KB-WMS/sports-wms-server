package com.kb.wms.store.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.store.application.port.in.StoreMemberUseCase;
import com.kb.wms.store.application.port.in.StoreUseCase;
import com.kb.wms.store.application.port.in.command.StoreMemberAssignCommand;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.domain.entity.StoreMember;
import com.kb.wms.store.exception.StoreErrorCode;

@WebMvcTest(StoreMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class StoreMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private StoreMemberUseCase storeMemberUseCase;
    @MockitoBean
    private StoreUseCase storeUseCase;

    private final Store store =
            Store.register("ST-GANGNAM", "강남점", "서울시 강남구", "김점주", "02-333-1234");

    private record TestAssignRequest(Long storeId, Long userId, String memberRole) {
    }

    @Test
    @DisplayName("관리자 배정에 성공하면 201과 지점명이 채워진 배정 정보를 반환한다")
    void assignManager_success() throws Exception {
        StoreMember member = StoreMember.assign(1L, 10L, "OWNER", null);
        when(storeMemberUseCase.assignManager(any(StoreMemberAssignCommand.class))).thenReturn(member);
        when(storeUseCase.getStore(1L)).thenReturn(store);

        mockMvc.perform(post("/api/v1/stores/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestAssignRequest(1L, 10L, "OWNER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(10L))
                .andExpect(jsonPath("$.data.storeName").value("강남점"));
    }

    @Test
    @DisplayName("이미 배정된 사용자를 다시 배정하면 409 ALREADY_ASSIGNED를 반환한다")
    void assignManager_alreadyAssigned() throws Exception {
        when(storeMemberUseCase.assignManager(any(StoreMemberAssignCommand.class)))
                .thenThrow(new BusinessException(StoreErrorCode.ALREADY_ASSIGNED));

        mockMvc.perform(post("/api/v1/stores/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestAssignRequest(1L, 10L, "OWNER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_ASSIGNED"));
    }

    @Test
    @DisplayName("storeId·userId 필터로 관리자 목록을 조회하면 해당 조건이 그대로 전달된다")
    void getManagers_withFilters_success() throws Exception {
        StoreMember member = StoreMember.assign(1L, 10L, "OWNER", null);
        when(storeMemberUseCase.getManagers(eq(1L), eq(10L))).thenReturn(List.of(member));
        when(storeUseCase.getStore(1L)).thenReturn(store);

        mockMvc.perform(get("/api/v1/stores/managers").param("storeId", "1").param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userId").value(10L));

        verify(storeMemberUseCase).getManagers(eq(1L), eq(10L));
    }

    @Test
    @DisplayName("필터 없이 관리자 목록을 조회하면 전체 배정을 조회한다")
    void getManagers_withoutFilters_returnsAll() throws Exception {
        StoreMember member = StoreMember.assign(1L, 10L, "OWNER", null);
        when(storeMemberUseCase.getManagers(null, null)).thenReturn(List.of(member));
        when(storeUseCase.getStore(1L)).thenReturn(store);

        mockMvc.perform(get("/api/v1/stores/managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userId").value(10L));
    }

    @Test
    @DisplayName("관리자 배정을 해제하면 200과 해제된 배정 ID를 반환한다")
    void releaseManager_success() throws Exception {
        mockMvc.perform(delete("/api/v1/stores/managers/{storeMemberId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeMemberId").value(1L));

        verify(storeMemberUseCase).releaseManager(1L);
    }

    @Test
    @DisplayName("존재하지 않는 배정을 해제하면 404 MEMBER_NOT_FOUND를 반환한다")
    void releaseManager_notFound() throws Exception {
        org.mockito.Mockito.doThrow(new BusinessException(StoreErrorCode.MEMBER_NOT_FOUND))
                .when(storeMemberUseCase).releaseManager(999L);

        mockMvc.perform(delete("/api/v1/stores/managers/{storeMemberId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }
}

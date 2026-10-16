package com.kb.wms.store.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.wms.common.paging.SortOrder;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.TestPages;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.store.application.port.in.StoreMemberUseCase;
import com.kb.wms.store.application.port.in.StoreUseCase;
import com.kb.wms.store.application.port.in.command.StoreMemberAssignCommand;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.application.port.in.result.StoreMemberView;
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
        StoreMemberView member = new StoreMemberView(5L, 1L, 10L, "김담당", "member01", "OWNER", null);
        when(storeMemberUseCase.assignManager(any(StoreMemberAssignCommand.class))).thenReturn(member);
        when(storeUseCase.getStore(1L)).thenReturn(store);

        mockMvc.perform(post("/api/v1/stores/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestAssignRequest(1L, 10L, "OWNER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(10L))
                .andExpect(jsonPath("$.data.userName").value("김담당"))
                .andExpect(jsonPath("$.data.loginId").value("member01"))
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
        StoreMemberView member = new StoreMemberView(5L, 1L, 10L, "김담당", "member01", "OWNER", null);
        when(storeMemberUseCase.getManagers(eq(1L), eq(10L), isNull(), any()))
                .thenReturn(TestPages.resultOf(List.of(member)));
        when(storeUseCase.getStore(1L)).thenReturn(store);

        mockMvc.perform(get("/api/v1/stores/managers").param("storeId", "1").param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userId").value(10L));

        verify(storeMemberUseCase).getManagers(eq(1L), eq(10L), isNull(), any());
    }

    @Test
    @DisplayName("필터 없이 관리자 목록을 조회하면 전체 배정을 조회한다")
    void getManagers_withoutFilters_returnsAll() throws Exception {
        StoreMemberView member = new StoreMemberView(5L, 1L, 10L, "김담당", "member01", "OWNER", null);
        when(storeMemberUseCase.getManagers(isNull(), isNull(), isNull(), any()))
                .thenReturn(TestPages.resultOf(List.of(member)));
        when(storeUseCase.getStore(1L)).thenReturn(store);

        mockMvc.perform(get("/api/v1/stores/managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userId").value(10L))
                .andExpect(jsonPath("$.data.items[0].userName").value("김담당"))
                .andExpect(jsonPath("$.data.items[0].loginId").value("member01"));
    }

    @Test
    @DisplayName("keyword로 관리자 목록을 조회하면 검색어가 그대로 전달된다")
    void getManagers_withKeyword() throws Exception {
        StoreMemberView member = new StoreMemberView(5L, 1L, 10L, "김담당", "member01", "OWNER", null);
        when(storeMemberUseCase.getManagers(isNull(), isNull(), eq("김담당"), any()))
                .thenReturn(TestPages.resultOf(List.of(member)));
        when(storeUseCase.getStore(1L)).thenReturn(store);

        mockMvc.perform(get("/api/v1/stores/managers").param("keyword", "김담당"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userName").value("김담당"));

        verify(storeMemberUseCase).getManagers(isNull(), isNull(), eq("김담당"), any());
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

    @Test
    @DisplayName("지점 담당자 목록은 page·size·sort를 페이지 조건으로 넘기고 pageInfo를 담아 반환한다")
    void getManagers_paging() throws Exception {
        when(storeMemberUseCase.getManagers(any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 2, 5, 11, "userName,asc"));

        mockMvc.perform(get("/api/v1/stores/managers").param("page", "2").param("size", "5").param("sort", "userName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.pageInfo.page").value(2))
                .andExpect(jsonPath("$.data.pageInfo.size").value(5))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(11))
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(3))
                .andExpect(jsonPath("$.data.pageInfo.sort").value("userName,asc"));

        ArgumentCaptor<PageCondition> captor = ArgumentCaptor.forClass(PageCondition.class);
        verify(storeMemberUseCase).getManagers(any(), any(), any(), captor.capture());
        PageCondition page = captor.getValue();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.orders()).extracting(SortOrder::property).containsExactly("u.name", "m.storeMemberId");
        assertThat(page.orders()).extracting(SortOrder::direction).containsOnly(SortDirection.ASC);
    }

    @Test
    @DisplayName("지점 담당자 목록은 page·size·sort를 생략하면 1페이지, 20건, 기본 정렬(assignedAt,desc)이다")
    void getManagers_defaultPaging() throws Exception {
        when(storeMemberUseCase.getManagers(any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 1, 20, 0, "assignedAt,desc"));

        mockMvc.perform(get("/api/v1/stores/managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(0))
                .andExpect(jsonPath("$.data.pageInfo.sort").value("assignedAt,desc"));

        ArgumentCaptor<PageCondition> captor = ArgumentCaptor.forClass(PageCondition.class);
        verify(storeMemberUseCase).getManagers(any(), any(), any(), captor.capture());
        assertThat(captor.getValue().page()).isEqualTo(1);
        assertThat(captor.getValue().size()).isEqualTo(20);
        assertThat(captor.getValue().sortLabel()).isEqualTo("assignedAt,desc");
    }

    @Test
    @DisplayName("지점 담당자 목록의 허용되지 않은 sort 필드와 범위를 벗어난 page·size는 400을 반환한다")
    void getManagers_invalidPaging() throws Exception {
        mockMvc.perform(get("/api/v1/stores/managers").param("sort", "unknown,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/stores/managers").param("sort", "userName,up"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/stores/managers").param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/stores/managers").param("page", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(storeMemberUseCase);
    }
}

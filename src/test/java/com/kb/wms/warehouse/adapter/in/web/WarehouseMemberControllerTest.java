package com.kb.wms.warehouse.adapter.in.web;

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

import java.math.BigDecimal;
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
import com.kb.wms.warehouse.application.port.in.WarehouseMemberUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseMemberAssignCommand;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMemberView;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

@WebMvcTest(WarehouseMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class WarehouseMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private WarehouseMemberUseCase warehouseMemberUseCase;
    @MockitoBean
    private WarehouseUseCase warehouseUseCase;

    private final Warehouse warehouse =
            Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.valueOf(1000));

    private record TestAssignRequest(Long warehouseId, Long userId, String memberRole) {
    }

    @Test
    @DisplayName("관리자 배정에 성공하면 201과 창고명이 채워진 배정 정보를 반환한다")
    void assignManager_success() throws Exception {
        WarehouseMemberView member = new WarehouseMemberView(5L, 1L, 10L, "김담당", "member01", "MANAGER", null);
        when(warehouseMemberUseCase.assignManager(any(WarehouseMemberAssignCommand.class))).thenReturn(member);
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(post("/api/v1/warehouses/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestAssignRequest(1L, 10L, "MANAGER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(10L))
                .andExpect(jsonPath("$.data.userName").value("김담당"))
                .andExpect(jsonPath("$.data.loginId").value("member01"))
                .andExpect(jsonPath("$.data.warehouseName").value("서울 물류센터"));
    }

    @Test
    @DisplayName("이미 배정된 사용자를 다시 배정하면 409 ALREADY_ASSIGNED를 반환한다")
    void assignManager_alreadyAssigned() throws Exception {
        when(warehouseMemberUseCase.assignManager(any(WarehouseMemberAssignCommand.class)))
                .thenThrow(new BusinessException(WarehouseErrorCode.ALREADY_ASSIGNED));

        mockMvc.perform(post("/api/v1/warehouses/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestAssignRequest(1L, 10L, "MANAGER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_ASSIGNED"));
    }

    @Test
    @DisplayName("warehouseId·userId 필터로 관리자 목록을 조회하면 해당 조건이 그대로 전달된다")
    void getManagers_withFilters_success() throws Exception {
        WarehouseMemberView member = new WarehouseMemberView(5L, 1L, 10L, "김담당", "member01", "MANAGER", null);
        when(warehouseMemberUseCase.getManagers(eq(1L), eq(10L), isNull(), any()))
                .thenReturn(TestPages.resultOf(List.of(member)));
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(get("/api/v1/warehouses/managers").param("warehouseId", "1").param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userId").value(10L));

        verify(warehouseMemberUseCase).getManagers(eq(1L), eq(10L), isNull(), any());
    }

    @Test
    @DisplayName("필터 없이 관리자 목록을 조회하면 전체 배정을 조회한다")
    void getManagers_withoutFilters_returnsAll() throws Exception {
        WarehouseMemberView member = new WarehouseMemberView(5L, 1L, 10L, "김담당", "member01", "MANAGER", null);
        when(warehouseMemberUseCase.getManagers(isNull(), isNull(), isNull(), any()))
                .thenReturn(TestPages.resultOf(List.of(member)));
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(get("/api/v1/warehouses/managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userId").value(10L))
                .andExpect(jsonPath("$.data.items[0].userName").value("김담당"))
                .andExpect(jsonPath("$.data.items[0].loginId").value("member01"));
    }

    @Test
    @DisplayName("keyword로 관리자 목록을 조회하면 검색어가 그대로 전달된다")
    void getManagers_withKeyword() throws Exception {
        WarehouseMemberView member = new WarehouseMemberView(5L, 1L, 10L, "김담당", "member01", "MANAGER", null);
        when(warehouseMemberUseCase.getManagers(isNull(), isNull(), eq("김담당"), any()))
                .thenReturn(TestPages.resultOf(List.of(member)));
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(get("/api/v1/warehouses/managers").param("keyword", "김담당"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userName").value("김담당"));

        verify(warehouseMemberUseCase).getManagers(isNull(), isNull(), eq("김담당"), any());
    }

    @Test
    @DisplayName("관리자 배정을 해제하면 200과 해제된 배정 ID를 반환한다")
    void releaseManager_success() throws Exception {
        mockMvc.perform(delete("/api/v1/warehouses/managers/{warehouseMemberId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warehouseMemberId").value(1L));

        verify(warehouseMemberUseCase).releaseManager(1L);
    }

    @Test
    @DisplayName("존재하지 않는 배정을 해제하면 404 MEMBER_NOT_FOUND를 반환한다")
    void releaseManager_notFound() throws Exception {
        org.mockito.Mockito.doThrow(new BusinessException(WarehouseErrorCode.MEMBER_NOT_FOUND))
                .when(warehouseMemberUseCase).releaseManager(999L);

        mockMvc.perform(delete("/api/v1/warehouses/managers/{warehouseMemberId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("창고 담당자 목록은 page·size·sort를 페이지 조건으로 넘기고 pageInfo를 담아 반환한다")
    void getManagers_paging() throws Exception {
        when(warehouseMemberUseCase.getManagers(any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 2, 5, 11, "userName,asc"));

        mockMvc.perform(get("/api/v1/warehouses/managers").param("page", "2").param("size", "5").param("sort", "userName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.pageInfo.page").value(2))
                .andExpect(jsonPath("$.data.pageInfo.size").value(5))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(11))
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(3))
                .andExpect(jsonPath("$.data.pageInfo.sort").value("userName,asc"));

        ArgumentCaptor<PageCondition> captor = ArgumentCaptor.forClass(PageCondition.class);
        verify(warehouseMemberUseCase).getManagers(any(), any(), any(), captor.capture());
        PageCondition page = captor.getValue();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.orders()).extracting(SortOrder::property).containsExactly("u.name", "m.warehouseMemberId");
        assertThat(page.orders()).extracting(SortOrder::direction).containsOnly(SortDirection.ASC);
    }

    @Test
    @DisplayName("창고 담당자 목록은 page·size·sort를 생략하면 1페이지, 20건, 기본 정렬(assignedAt,desc)이다")
    void getManagers_defaultPaging() throws Exception {
        when(warehouseMemberUseCase.getManagers(any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 1, 20, 0, "assignedAt,desc"));

        mockMvc.perform(get("/api/v1/warehouses/managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(0))
                .andExpect(jsonPath("$.data.pageInfo.sort").value("assignedAt,desc"));

        ArgumentCaptor<PageCondition> captor = ArgumentCaptor.forClass(PageCondition.class);
        verify(warehouseMemberUseCase).getManagers(any(), any(), any(), captor.capture());
        assertThat(captor.getValue().page()).isEqualTo(1);
        assertThat(captor.getValue().size()).isEqualTo(20);
        assertThat(captor.getValue().sortLabel()).isEqualTo("assignedAt,desc");
    }

    @Test
    @DisplayName("창고 담당자 목록의 허용되지 않은 sort 필드와 범위를 벗어난 page·size는 400을 반환한다")
    void getManagers_invalidPaging() throws Exception {
        mockMvc.perform(get("/api/v1/warehouses/managers").param("sort", "unknown,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/warehouses/managers").param("sort", "userName,up"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/warehouses/managers").param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/warehouses/managers").param("page", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(warehouseMemberUseCase);
    }
}

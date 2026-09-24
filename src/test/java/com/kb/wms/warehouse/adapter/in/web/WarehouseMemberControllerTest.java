package com.kb.wms.warehouse.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.kb.wms.warehouse.application.port.in.WarehouseMemberUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseMemberAssignCommand;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.domain.entity.WarehouseMember;
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
        WarehouseMember member = WarehouseMember.assign(1L, 10L, "MANAGER", null);
        when(warehouseMemberUseCase.assignManager(any(WarehouseMemberAssignCommand.class))).thenReturn(member);
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(post("/api/v1/warehouses/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestAssignRequest(1L, 10L, "MANAGER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(10L))
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
                .andExpect(jsonPath("$.error_code").value("ALREADY_ASSIGNED"));
    }

    @Test
    @DisplayName("warehouseId·userId 필터로 관리자 목록을 조회하면 해당 조건이 그대로 전달된다")
    void getManagers_withFilters_success() throws Exception {
        WarehouseMember member = WarehouseMember.assign(1L, 10L, "MANAGER", null);
        when(warehouseMemberUseCase.getManagers(eq(1L), eq(10L))).thenReturn(List.of(member));
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(get("/api/v1/warehouses/managers").param("warehouseId", "1").param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userId").value(10L));

        verify(warehouseMemberUseCase).getManagers(eq(1L), eq(10L));
    }

    @Test
    @DisplayName("필터 없이 관리자 목록을 조회하면 전체 배정을 조회한다")
    void getManagers_withoutFilters_returnsAll() throws Exception {
        WarehouseMember member = WarehouseMember.assign(1L, 10L, "MANAGER", null);
        when(warehouseMemberUseCase.getManagers(null, null)).thenReturn(List.of(member));
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(get("/api/v1/warehouses/managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userId").value(10L));
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
                .andExpect(jsonPath("$.error_code").value("MEMBER_NOT_FOUND"));
    }
}

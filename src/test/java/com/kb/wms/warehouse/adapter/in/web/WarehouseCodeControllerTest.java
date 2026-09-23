package com.kb.wms.warehouse.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.warehouse.application.port.in.WarehouseCodeUseCase;
import com.kb.wms.warehouse.application.port.in.result.CodeItem;

@WebMvcTest(WarehouseCodeController.class)
@AutoConfigureMockMvc(addFilters = false)
class WarehouseCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarehouseCodeUseCase warehouseCodeUseCase;

    @Test
    @DisplayName("담당 역할 코드 목록을 조회하면 200과 items 배열로 감싼 응답을 반환한다")
    void getManagementTypes_success() throws Exception {
        when(warehouseCodeUseCase.getManagementTypes()).thenReturn(List.of(new CodeItem("MANAGER", "창고 관리자")));

        mockMvc.perform(get("/api/v1/warehouses/management-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("MANAGER"))
                .andExpect(jsonPath("$.data.items[0].name").value("창고 관리자"));
    }

    @Test
    @DisplayName("구역 유형 코드 목록을 조회하면 200과 items 배열로 감싼 응답을 반환한다")
    void getSectionTypes_success() throws Exception {
        when(warehouseCodeUseCase.getSectionTypes()).thenReturn(List.of(new CodeItem("ZONE", "구역"), new CodeItem("RACK", "랙")));

        mockMvc.perform(get("/api/v1/warehouses/section-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("ZONE"))
                .andExpect(jsonPath("$.data.items[1].code").value("RACK"));
    }
}

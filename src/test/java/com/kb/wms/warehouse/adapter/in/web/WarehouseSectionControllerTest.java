package com.kb.wms.warehouse.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.kb.wms.warehouse.application.port.in.WarehouseSectionUseCase;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseSectionUpdateCommand;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

@WebMvcTest(WarehouseSectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class WarehouseSectionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private WarehouseSectionUseCase warehouseSectionUseCase;
    @MockitoBean
    private WarehouseUseCase warehouseUseCase;

    private final Warehouse warehouse =
            Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.valueOf(1000));

    private record TestRegisterRequest(Long warehouseId, Long parentSectionId, String sectionCode,
                                        String sectionName, String sectionType, BigDecimal capacity) {
    }

    private record TestUpdateRequest(String sectionCode, String sectionName, String sectionType,
                                      BigDecimal capacity, Long warehouseId, Long parentSectionId, Boolean isActive) {
    }

    @Test
    @DisplayName("구역 등록에 성공하면 201과 창고명이 채워진 구역을 반환한다")
    void registerSection_success() throws Exception {
        WarehouseSection section = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseSectionUseCase.registerSection(any(WarehouseSectionRegisterCommand.class))).thenReturn(section);
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(post("/api/v1/warehouses/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestRegisterRequest(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sectionCode").value("A-01"))
                .andExpect(jsonPath("$.data.warehouseName").value("서울 물류센터"))
                .andExpect(jsonPath("$.data.parentSectionCode").doesNotExist());
    }

    @Test
    @DisplayName("허용되지 않은 구역 유형으로 등록하면 400 VALIDATION_ERROR를 반환한다")
    void registerSection_invalidType_returnsValidationError() throws Exception {
        when(warehouseSectionUseCase.registerSection(any(WarehouseSectionRegisterCommand.class)))
                .thenThrow(new BusinessException(com.kb.wms.common.exception.ErrorCode.VALIDATION_ERROR,
                        "허용되지 않은 구역 유형입니다."));

        mockMvc.perform(post("/api/v1/warehouses/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestRegisterRequest(1L, null, "A-01", "1구역", "INVALID", BigDecimal.valueOf(100)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("전체 구역 목록을 조회하면 200과 목록을 반환한다")
    void getAllSections_success() throws Exception {
        WarehouseSection section = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseSectionUseCase.getSections(null)).thenReturn(List.of(section));
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(get("/api/v1/warehouses/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].sectionCode").value("A-01"));
    }

    @Test
    @DisplayName("창고별 구역 목록을 조회하면 200과 해당 창고의 구역만 반환한다")
    void getSectionsByWarehouse_success() throws Exception {
        WarehouseSection section = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        when(warehouseSectionUseCase.getSections(1L)).thenReturn(List.of(section));
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(get("/api/v1/warehouses/{warehouseId}/sections", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].sectionCode").value("A-01"))
                .andExpect(jsonPath("$.data.items[0].warehouseId").value(1L));
    }

    @Test
    @DisplayName("계층 구조가 있는 구역을 조회하면 parentSectionCode가 함께 채워진다")
    void getSection_withParent_resolvesParentSectionCode() throws Exception {
        WarehouseSection parent = WarehouseSection.register(1L, null, "A", "A구역", "ZONE", BigDecimal.valueOf(500));
        WarehouseSection child = WarehouseSection.register(1L, 10L, "A-01", "A-1랙", "RACK", BigDecimal.valueOf(50));
        when(warehouseSectionUseCase.getSection(2L)).thenReturn(child);
        when(warehouseSectionUseCase.getSection(10L)).thenReturn(parent);
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(get("/api/v1/warehouses/sections/{sectionId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentSectionCode").value("A"))
                .andExpect(jsonPath("$.data.warehouseName").value("서울 물류센터"));
    }

    @Test
    @DisplayName("존재하지 않는 구역을 조회하면 404 SECTION_NOT_FOUND를 반환한다")
    void getSection_notFound() throws Exception {
        when(warehouseSectionUseCase.getSection(999L)).thenThrow(new BusinessException(WarehouseErrorCode.SECTION_NOT_FOUND));

        mockMvc.perform(get("/api/v1/warehouses/sections/{sectionId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SECTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("구역을 수정하면 200과 수정된 구역을 반환한다")
    void updateSection_success() throws Exception {
        WarehouseSection updated = WarehouseSection.register(1L, null, "A-01", "새 이름", "ZONE", BigDecimal.valueOf(100));
        when(warehouseSectionUseCase.updateSection(eq(1L), any(WarehouseSectionUpdateCommand.class))).thenReturn(updated);
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(patch("/api/v1/warehouses/sections/{sectionId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestUpdateRequest(null, "새 이름", null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sectionName").value("새 이름"));
    }

    @Test
    @DisplayName("warehouseId·parentSectionId·isActive 필드를 함께 보내 수정을 요청하면 400 VALIDATION_ERROR를 반환한다")
    void updateSection_immutableFields_returnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/warehouses/sections/{sectionId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestUpdateRequest(null, null, null, null, 2L, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("구역을 비활성화하면 200과 비활성화된 구역을 반환한다")
    void deactivateSection_success() throws Exception {
        WarehouseSection section = WarehouseSection.register(1L, null, "A-01", "1구역", "ZONE", BigDecimal.valueOf(100));
        section.deactivate();
        when(warehouseSectionUseCase.deactivateSection(1L)).thenReturn(section);
        when(warehouseUseCase.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(patch("/api/v1/warehouses/sections/{sectionId}/deactivate", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }
}

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
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseRegisterCommand;
import com.kb.wms.warehouse.application.port.in.command.WarehouseUpdateCommand;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMembershipSummary;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

@WebMvcTest(WarehouseController.class)
@AutoConfigureMockMvc(addFilters = false)
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private WarehouseUseCase warehouseUseCase;

    private record TestRegisterRequest(String warehouseCode, String warehouseName, String address,
                                        String contactNumber, BigDecimal totalCapacity) {
    }

    private record TestUpdateRequest(String warehouseName, String address, String contactNumber,
                                      BigDecimal totalCapacity, String warehouseCode, Boolean isActive) {
    }

    @Test
    @DisplayName("창고 등록에 성공하면 201과 등록된 창고를 반환한다")
    void registerWarehouse_success() throws Exception {
        Warehouse warehouse = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        when(warehouseUseCase.registerWarehouse(any(WarehouseRegisterCommand.class))).thenReturn(warehouse);

        mockMvc.perform(post("/api/v1/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRegisterRequest(
                                "WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.warehouseCode").value("WH-001"))
                .andExpect(jsonPath("$.data.warehouseName").value("서울 물류센터"));
    }

    @Test
    @DisplayName("창고 코드가 중복되면 409 DUPLICATE_WAREHOUSE_CODE를 반환한다")
    void registerWarehouse_duplicateCode() throws Exception {
        when(warehouseUseCase.registerWarehouse(any(WarehouseRegisterCommand.class)))
                .thenThrow(new BusinessException(WarehouseErrorCode.DUPLICATE_WAREHOUSE_CODE));

        mockMvc.perform(post("/api/v1/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestRegisterRequest(
                                "WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("DUPLICATE_WAREHOUSE_CODE"));
    }

    @Test
    @DisplayName("창고 목록을 조회하면 200과 목록을 반환한다")
    void getWarehouses_success() throws Exception {
        Warehouse warehouse = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        when(warehouseUseCase.getWarehouses()).thenReturn(List.of(warehouse));

        mockMvc.perform(get("/api/v1/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].warehouseCode").value("WH-001"));
    }

    @Test
    @DisplayName("내 소속 창고를 조회하면 200과 items 배열로 감싼 응답을 반환한다")
    void getMyWarehouses_success() throws Exception {
        WarehouseMembershipSummary summary = new WarehouseMembershipSummary(
                1L, "WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN,
                true, 100L, "MANAGER", null);
        when(warehouseUseCase.getMyWarehouses(eq(10L))).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/warehouses/my").param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].warehouseCode").value("WH-001"))
                .andExpect(jsonPath("$.data.items[0].memberRole").value("MANAGER"));
    }

    @Test
    @DisplayName("존재하지 않는 창고를 조회하면 404 WAREHOUSE_NOT_FOUND를 반환한다")
    void getWarehouse_notFound() throws Exception {
        when(warehouseUseCase.getWarehouse(999L)).thenThrow(new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/warehouses/{warehouseId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("WAREHOUSE_NOT_FOUND"));
    }

    @Test
    @DisplayName("창고를 수정하면 200과 수정된 창고를 반환한다")
    void updateWarehouse_success() throws Exception {
        Warehouse updated = Warehouse.register("WH-001", "새 이름", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        when(warehouseUseCase.updateWarehouse(eq(1L), any(WarehouseUpdateCommand.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/warehouses/{warehouseId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestUpdateRequest("새 이름", null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warehouseName").value("새 이름"));
    }

    @Test
    @DisplayName("warehouseCode·isActive 필드를 함께 보내 수정을 요청하면 400 VALIDATION_ERROR를 반환한다")
    void updateWarehouse_immutableFields_returnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/warehouses/{warehouseId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestUpdateRequest(null, null, null, null, "WH-999", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("창고를 비활성화하면 200과 비활성화된 창고를 반환한다")
    void deactivateWarehouse_success() throws Exception {
        Warehouse warehouse = Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.TEN);
        warehouse.deactivate();
        when(warehouseUseCase.deactivateWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(patch("/api/v1/warehouses/{warehouseId}/deactivate", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }
}

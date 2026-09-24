package com.kb.wms.inventory.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.LotUseCase;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.domain.entity.Warehouse;

@WebMvcTest(LotController.class)
@AutoConfigureMockMvc(addFilters = false)
class LotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LotUseCase lotUseCase;
    @MockitoBean
    private InventoryQueryUseCase inventoryQueryUseCase;
    @MockitoBean
    private WarehouseUseCase warehouseUseCase;

    @Test
    @DisplayName("GET /api/v1/lots: 로트 목록을 반환한다")
    void getLots_success() throws Exception {
        LotSummary summary = new LotSummary(1L, "LOT-001", 2L, "SKU-001", "상품A", 3L,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), LotStatus.AVAILABLE,
                BigDecimal.TEN, null, null);
        when(lotUseCase.getLots(new LotSearchCondition(2L, null, null, null))).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/lots").param("skuId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].lotNumber").value("LOT-001"))
                .andExpect(jsonPath("$.data.items[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/v1/lots: 존재하지 않는 skuId로 필터링하면 404 NOT_FOUND를 반환한다")
    void getLots_skuNotFound() throws Exception {
        when(lotUseCase.getLots(any(LotSearchCondition.class)))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "SKU를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/lots").param("skuId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/lots/{lotId}: 로트 상세와 구역별 재고, 창고명을 함께 반환한다")
    void getLot_success() throws Exception {
        LotSummary summary = new LotSummary(1L, "LOT-001", 2L, "SKU-001", "상품A", 3L,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), LotStatus.AVAILABLE,
                BigDecimal.TEN, null, null);
        InventoryLotView view = new InventoryLotView(10L, 1L, "LOT-001", 2L, "SKU-001", "상품A",
                4L, 5L, "A-01", "1구역", 100L, 20L, 80L, QualityStatus.AVAILABLE, null, null);
        Warehouse warehouse =
                Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.valueOf(1000));
        when(lotUseCase.getLot(1L)).thenReturn(summary);
        when(inventoryQueryUseCase.getInventoriesByLot(InventoryLotSearchCondition.ofLot(1L)))
                .thenReturn(List.of(view));
        when(warehouseUseCase.getWarehouse(4L)).thenReturn(warehouse);

        mockMvc.perform(get("/api/v1/lots/{lotId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lotNumber").value("LOT-001"))
                .andExpect(jsonPath("$.data.inbounds").isArray())
                .andExpect(jsonPath("$.data.inbounds").isEmpty())
                .andExpect(jsonPath("$.data.inventory[0].inventoryLotId").value(10))
                .andExpect(jsonPath("$.data.inventory[0].warehouseName").value("서울 물류센터"))
                .andExpect(jsonPath("$.data.inventory[0].sectionCode").value("A-01"));
    }

    @Test
    @DisplayName("GET /api/v1/lots/{lotId}: 존재하지 않으면 404 NOT_FOUND를 반환한다")
    void getLot_notFound() throws Exception {
        when(lotUseCase.getLot(999L)).thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "로트를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/lots/{lotId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}

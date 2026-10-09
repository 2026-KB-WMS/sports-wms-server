package com.kb.wms.inventory.adapter.in.web;

import static com.kb.wms.common.security.TestAuth.signInAsHqAdmin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.LotUseCase;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.application.port.in.result.LotInboundView;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.inventory.exception.InventoryErrorCode;
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

    @BeforeEach
    void signIn() {
        signInAsHqAdmin();
    }

    @Test
    @DisplayName("GET /api/v1/lots: 로트 목록을 반환한다")
    void getLots_success() throws Exception {
        LotSummary summary = new LotSummary(1L, "LOT-001", 2L, "SKU-001", "상품A", 3L, "한빛식품",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), LotStatus.AVAILABLE,
                BigDecimal.TEN, null, null);
        when(lotUseCase.getLots(eq(LotSearchCondition.unscoped(2L, null, null, null)), any())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/lots").param("skuId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].lotNumber").value("LOT-001"))
                .andExpect(jsonPath("$.data.items[0].supplierName").value("한빛식품"))
                .andExpect(jsonPath("$.data.items[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/v1/lots: 존재하지 않는 skuId로 필터링하면 404 SKU_NOT_FOUND를 반환한다")
    void getLots_skuNotFound() throws Exception {
        when(lotUseCase.getLots(any(LotSearchCondition.class), any()))
                .thenThrow(new BusinessException(InventoryErrorCode.SKU_NOT_FOUND));

        mockMvc.perform(get("/api/v1/lots").param("skuId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SKU_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/lots: 존재하지 않는 supplierId로 필터링하면 404 SUPPLIER_NOT_FOUND를 반환한다")
    void getLots_supplierNotFound() throws Exception {
        when(lotUseCase.getLots(any(LotSearchCondition.class), any()))
                .thenThrow(new BusinessException(InventoryErrorCode.SUPPLIER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/lots").param("supplierId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SUPPLIER_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/lots/{lotId}: 로트 상세와 구역별 재고, 창고명을 함께 반환한다")
    void getLot_success() throws Exception {
        LotSummary summary = new LotSummary(1L, "LOT-001", 2L, "SKU-001", "상품A", 3L, "한빛식품",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), LotStatus.AVAILABLE,
                BigDecimal.TEN, null, null);
        InventoryLotView view = new InventoryLotView(10L, 1L, "LOT-001", 2L, "SKU-001", "상품A",
                4L, 5L, "A-01", "1구역", 100L, 20L, 80L, QualityStatus.AVAILABLE, null, null);
        Warehouse warehouse =
                Warehouse.register("WH-001", "서울 물류센터", "서울시 강남구", "02-1234-5678", BigDecimal.valueOf(1000));
        when(lotUseCase.getLot(eq(1L), any())).thenReturn(summary);
        when(inventoryQueryUseCase.getInventoriesByLot(eq(InventoryLotSearchCondition.ofLot(1L)), any()))
                .thenReturn(List.of(view));
        when(warehouseUseCase.getWarehouse(4L)).thenReturn(warehouse);
        when(lotUseCase.getLotInbounds(eq(1L), any())).thenReturn(List.of(new LotInboundView(
                7L, "IB-20261001-0001", 4L, LocalDateTime.of(2026, 10, 1, 14, 30),
                100L, 95L, 5L, new BigDecimal("1200.00"))));

        mockMvc.perform(get("/api/v1/lots/{lotId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lotNumber").value("LOT-001"))
                .andExpect(jsonPath("$.data.supplierName").value("한빛식품"))
                .andExpect(jsonPath("$.data.inbounds[0].inboundId").value(7))
                .andExpect(jsonPath("$.data.inbounds[0].inboundNo").value("IB-20261001-0001"))
                .andExpect(jsonPath("$.data.inbounds[0].warehouseId").value(4))
                .andExpect(jsonPath("$.data.inbounds[0].receivedQuantity").value(100))
                .andExpect(jsonPath("$.data.inbounds[0].acceptedQuantity").value(95))
                .andExpect(jsonPath("$.data.inbounds[0].defectiveQuantity").value(5))
                .andExpect(jsonPath("$.data.inbounds[0].receivedUnitPrice").value(1200.00))
                .andExpect(jsonPath("$.data.inventory[0].inventoryLotId").value(10))
                .andExpect(jsonPath("$.data.inventory[0].warehouseName").value("서울 물류센터"))
                .andExpect(jsonPath("$.data.inventory[0].sectionCode").value("A-01"));
    }

    @Test
    @DisplayName("GET /api/v1/lots/{lotId}: 입고 이력이 없으면 inbounds는 빈 배열이다")
    void getLot_noInbounds_returnsEmptyArray() throws Exception {
        LotSummary summary = new LotSummary(1L, "LOT-001", 2L, "SKU-001", "상품A", 3L, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), LotStatus.AVAILABLE,
                BigDecimal.TEN, null, null);
        when(lotUseCase.getLot(eq(1L), any())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/lots/{lotId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inbounds").isArray())
                .andExpect(jsonPath("$.data.inbounds").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/lots/{lotId}: 존재하지 않으면 404 LOT_NOT_FOUND를 반환한다")
    void getLot_notFound() throws Exception {
        when(lotUseCase.getLot(eq(999L), any())).thenThrow(new BusinessException(InventoryErrorCode.LOT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/lots/{lotId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("LOT_NOT_FOUND"));
    }
}

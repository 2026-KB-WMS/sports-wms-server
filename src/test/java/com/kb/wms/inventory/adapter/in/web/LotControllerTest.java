package com.kb.wms.inventory.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static com.kb.wms.common.security.TestAuth.signInAsHqAdmin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.common.paging.SortOrder;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.TestPages;
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
import com.kb.wms.warehouse.application.port.in.result.WarehouseView;

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
        when(lotUseCase.getLots(eq(LotSearchCondition.unscoped(2L, null, null, null)), any(), any())).thenReturn(TestPages.resultOf(List.of(summary)));

        mockMvc.perform(get("/api/v1/lots").param("skuId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].lotNumber").value("LOT-001"))
                .andExpect(jsonPath("$.data.items[0].supplierName").value("한빛식품"))
                .andExpect(jsonPath("$.data.items[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/v1/lots: 존재하지 않는 skuId로 필터링하면 404 SKU_NOT_FOUND를 반환한다")
    void getLots_skuNotFound() throws Exception {
        when(lotUseCase.getLots(any(LotSearchCondition.class), any(), any()))
                .thenThrow(new BusinessException(InventoryErrorCode.SKU_NOT_FOUND));

        mockMvc.perform(get("/api/v1/lots").param("skuId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SKU_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/lots: 존재하지 않는 supplierId로 필터링하면 404 SUPPLIER_NOT_FOUND를 반환한다")
    void getLots_supplierNotFound() throws Exception {
        when(lotUseCase.getLots(any(LotSearchCondition.class), any(), any()))
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
        when(lotUseCase.getLot(eq(1L), any())).thenReturn(summary);
        when(inventoryQueryUseCase.getInventoriesByLot(eq(InventoryLotSearchCondition.ofLot(1L)), any()))
                .thenReturn(List.of(view));
        when(warehouseUseCase.getWarehouseView(4L))
                .thenReturn(new WarehouseView(4L, "서울 물류센터", true));
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

    @Test
    @DisplayName("PATCH /api/v1/lots/{lotId}/status: 상태를 바꾸고 변경된 로트를 반환한다")
    void changeStatus_success() throws Exception {
        LotSummary summary = new LotSummary(1L, "LOT-001", 2L, "SKU-001", "상품A", 3L, "한빛식품",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), LotStatus.QUARANTINED,
                BigDecimal.TEN, null, null);
        when(lotUseCase.getLot(eq(1L), any())).thenReturn(summary);

        mockMvc.perform(patch("/api/v1/lots/{lotId}/status", 1L)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"QUARANTINED\",\"reason\":\"품질 이상\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lotId").value(1))
                .andExpect(jsonPath("$.data.status").value("QUARANTINED"));

        verify(lotUseCase).changeLotStatus(eq(1L), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/lots/{lotId}/status: 상태나 사유가 없으면 400이다")
    void changeStatus_validation() throws Exception {
        mockMvc.perform(patch("/api/v1/lots/{lotId}/status", 1L)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"사유\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(patch("/api/v1/lots/{lotId}/status", 1L)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISPOSED\",\"reason\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PATCH /api/v1/lots/{lotId}/status: 허용되지 않은 전이는 409로 응답한다")
    void changeStatus_conflict() throws Exception {
        doThrow(new BusinessException(InventoryErrorCode.INVALID_LOT_STATUS_TRANSITION))
                .when(lotUseCase).changeLotStatus(eq(1L), any());

        mockMvc.perform(patch("/api/v1/lots/{lotId}/status", 1L)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"AVAILABLE\",\"reason\":\"사유\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_LOT_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("로트 목록은 page·size·sort를 페이지 조건으로 넘기고 pageInfo를 담아 반환한다")
    void lots_paging() throws Exception {
        when(lotUseCase.getLots(any(LotSearchCondition.class), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 2, 5, 11, "lotNumber,asc"));

        mockMvc.perform(get("/api/v1/lots").param("page", "2").param("size", "5").param("sort", "lotNumber,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.pageInfo.page").value(2))
                .andExpect(jsonPath("$.data.pageInfo.size").value(5))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(11))
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(3))
                .andExpect(jsonPath("$.data.pageInfo.sort").value("lotNumber,asc"));

        ArgumentCaptor<PageCondition> captor = ArgumentCaptor.forClass(PageCondition.class);
        verify(lotUseCase).getLots(any(LotSearchCondition.class), captor.capture(), any());
        PageCondition page = captor.getValue();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.orders()).extracting(SortOrder::property).containsExactly("l.lotNumber", "l.lotId");
        assertThat(page.orders()).extracting(SortOrder::direction).containsOnly(SortDirection.ASC);
    }

    @Test
    @DisplayName("로트 목록은 page·size·sort를 생략하면 1페이지, 20건, 기본 정렬(expiryDate,asc)이다")
    void lots_defaultPaging() throws Exception {
        when(lotUseCase.getLots(any(LotSearchCondition.class), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 1, 20, 0, "expiryDate,asc"));

        mockMvc.perform(get("/api/v1/lots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(0))
                .andExpect(jsonPath("$.data.pageInfo.sort").value("expiryDate,asc"));

        ArgumentCaptor<PageCondition> captor = ArgumentCaptor.forClass(PageCondition.class);
        verify(lotUseCase).getLots(any(LotSearchCondition.class), captor.capture(), any());
        assertThat(captor.getValue().page()).isEqualTo(1);
        assertThat(captor.getValue().size()).isEqualTo(20);
        assertThat(captor.getValue().sortLabel()).isEqualTo("expiryDate,asc");
    }

    @Test
    @DisplayName("로트 목록의 허용되지 않은 sort 필드와 범위를 벗어난 page·size는 400을 반환한다")
    void lots_invalidPaging() throws Exception {
        mockMvc.perform(get("/api/v1/lots").param("sort", "unknown,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/lots").param("sort", "lotNumber,up"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/lots").param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/lots").param("page", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lotUseCase);
    }
}

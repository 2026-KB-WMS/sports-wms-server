package com.kb.wms.inventory.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.InventoryAdjustmentUseCase;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.command.InventoryAdjustCommand;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventoryTransactionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LowStockSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryAdjustmentResult;
import com.kb.wms.inventory.application.port.in.result.InventoryDetail;
import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.application.port.in.result.InventorySkuSummary;
import com.kb.wms.inventory.application.port.in.result.InventoryTransactionView;
import com.kb.wms.inventory.application.port.in.result.LowStockItem;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.entity.InventoryTransaction;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;
import com.kb.wms.inventory.exception.InventoryErrorCode;

/**
 * InventoryController의 조회·조정 엔드포인트 전체(9개)에 대한 웹 계층 테스트.
 * #55에서 추가된 MethodArgumentTypeMismatchException 처리 검증은 그대로 유지한다.
 */
@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private InventoryQueryUseCase inventoryQueryUseCase;
    @MockitoBean
    private InventoryAdjustmentUseCase inventoryAdjustmentUseCase;

    private record TestAdjustRequest(Long inventoryLotId, Long beforeQuantity, Long afterQuantity, String reason) {
    }

    @Test
    @DisplayName("inventoryId가 숫자가 아니면 400 VALIDATION_ERROR를 반환한다")
    void getInventory_nonNumericId_returnsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{inventoryId}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/v1/inventory: SKU 기준 재고 요약 목록을 반환한다")
    void getInventories_success() throws Exception {
        InventorySkuSummary summary = new InventorySkuSummary(1L, "SKU-001", "상품A", "EA", 100L, 80L, 20L, 0L);
        when(inventoryQueryUseCase.getInventories(new InventorySearchCondition(1L, 2L, "상품")))
                .thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/inventory")
                        .param("skuId", "1").param("warehouseId", "2").param("keyword", "상품"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].skuCode").value("SKU-001"))
                .andExpect(jsonPath("$.data.items[0].totalQuantity").value(100))
                .andExpect(jsonPath("$.data.items[0].availableQuantity").value(80));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/by-lot: 로트·구역 단위 재고 목록을 반환한다")
    void getInventoriesByLot_success() throws Exception {
        InventoryLotView view = new InventoryLotView(1L, 2L, "LOT-001", 3L, "SKU-001", "상품A",
                4L, 5L, "A-01", "1구역", 100L, 20L, 80L, QualityStatus.AVAILABLE, null, null);
        when(inventoryQueryUseCase.getInventoriesByLot(any(InventoryLotSearchCondition.class)))
                .thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/inventory/by-lot").param("sectionId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].inventoryLotId").value(1))
                .andExpect(jsonPath("$.data.items[0].sectionCode").value("A-01"))
                .andExpect(jsonPath("$.data.items[0].availableQuantity").value(80));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/{inventoryId}: 재고 상세를 반환한다")
    void getInventory_success() throws Exception {
        InventoryDetail detail = new InventoryDetail(1L, 1L, "서울센터", 1L, "A-01", "1구역", 1L, "SKU-001", "상품A",
                "EA", 1L, "LOT-001", null, null, null, null, null, 100L, 20L, 80L, QualityStatus.AVAILABLE,
                null, null, null);
        when(inventoryQueryUseCase.getInventory(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/inventory/{inventoryId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skuCode").value("SKU-001"))
                .andExpect(jsonPath("$.data.onHandQuantity").value(100))
                .andExpect(jsonPath("$.data.availableQuantity").value(80));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/{inventoryId}: 존재하지 않으면 404 NOT_FOUND를 반환한다")
    void getInventory_notFound() throws Exception {
        when(inventoryQueryUseCase.getInventory(999L))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "재고를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/inventory/{inventoryId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/low-stock: 안전 재고 이하 SKU 목록을 반환한다")
    void getLowStock_success() throws Exception {
        LowStockItem item = new LowStockItem(1L, "SKU-001", "상품A", "EA", 50L, 30L, 20L);
        when(inventoryQueryUseCase.getLowStock(new LowStockSearchCondition(null, null)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/inventory/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].skuCode").value("SKU-001"))
                .andExpect(jsonPath("$.data.items[0].shortageQuantity").value(20));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/transactions: 조건에 맞는 이력 목록을 반환한다")
    void getTransactions_success() throws Exception {
        InventoryTransactionView view = new InventoryTransactionView(1L, 2L, 3L, 4L, "A-01", 5L, "SKU-001",
                6L, "LOT-001", TransactionType.ADJUSTMENT, -3L, 100L, 97L, ReferenceType.ADJUSTMENT, null,
                "실사 조정", 9L, LocalDateTime.of(2026, 1, 1, 0, 0));
        when(inventoryQueryUseCase.getTransactions(any(InventoryTransactionSearchCondition.class)))
                .thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/inventory/transactions").param("skuId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].transactionId").value(1))
                .andExpect(jsonPath("$.data.items[0].transactionType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.data.items[0].quantityDelta").value(-3));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/{inventoryId}/transactions: 해당 재고의 이력만 반환한다")
    void getTransactionsOf_success() throws Exception {
        InventoryTransactionView view = new InventoryTransactionView(1L, 1L, 3L, 4L, "A-01", 5L, "SKU-001",
                6L, "LOT-001", TransactionType.INBOUND, 50L, 50L, 100L, ReferenceType.INBOUND, 7L,
                null, 9L, LocalDateTime.of(2026, 1, 1, 0, 0));
        when(inventoryQueryUseCase.getTransactionsOf(eq(1L), any(InventoryTransactionSearchCondition.class)))
                .thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/inventory/{inventoryId}/transactions", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inventoryLotId").value(1))
                .andExpect(jsonPath("$.data.items[0].transactionType").value("INBOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/adjustments: 조정에 성공하면 201과 조정 결과를 반환한다")
    void adjust_success() throws Exception {
        InventoryLot inventoryLot = InventoryLot.open(10L, 20L, QualityStatus.AVAILABLE);
        inventoryLot.increase(100L);
        inventoryLot.increase(20L);
        InventoryTransaction transaction = InventoryTransaction.record(
                1L, TransactionType.ADJUSTMENT, 100L, 120L, ReferenceType.ADJUSTMENT, null, "실사 반영", 9L);
        when(inventoryAdjustmentUseCase.adjust(any(InventoryAdjustCommand.class)))
                .thenReturn(new InventoryAdjustmentResult(transaction, inventoryLot));

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestAdjustRequest(1L, 100L, 120L, "실사 반영"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.afterQuantity").value(120))
                .andExpect(jsonPath("$.data.inventory.onHandQuantity").value(120));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/adjustments: 필수 값이 없으면 400 VALIDATION_ERROR를 반환한다")
    void adjust_missingRequiredFields_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestAdjustRequest(1L, 100L, 120L, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/adjustments: 화면 수량이 현재 값과 다르면 409 STALE_QUANTITY를 반환한다")
    void adjust_staleQuantity_returnsConflict() throws Exception {
        when(inventoryAdjustmentUseCase.adjust(any(InventoryAdjustCommand.class)))
                .thenThrow(new BusinessException(InventoryErrorCode.STALE_QUANTITY));

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestAdjustRequest(1L, 99L, 120L, "실사 반영"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STALE_QUANTITY"));
    }
}

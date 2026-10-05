package com.kb.wms.outbound.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.outbound.application.port.in.StockAllocationUseCase;
import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;
import com.kb.wms.outbound.application.port.in.command.StockAllocationReleaseCommand;
import com.kb.wms.outbound.application.port.in.query.StockAllocationSearchCondition;
import com.kb.wms.outbound.application.port.in.result.StockAllocateResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationDetail;
import com.kb.wms.outbound.application.port.in.result.StockAllocationReleaseResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationSummary;
import com.kb.wms.outbound.application.port.in.result.StockAllocationView;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.exception.OutboundErrorCode;

@WebMvcTest(AllocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AllocationControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 14, 0);

    @Autowired MockMvc mockMvc;
    @MockitoBean StockAllocationUseCase useCase;

    private StockAllocationSummary summary() {
        return new StockAllocationSummary(500L, 3L, "SO-20261005-0001", 31L, 2L, 5L, "SKU-A", "상품 A", 900L, 9L,
                "LOT-A", LocalDate.of(2026, 12, 31), 4L, "A-01", 3L, 0L, AllocationStatus.ALLOCATED, NOW, null);
    }

    @Test
    @DisplayName("할당 생성은 201과 items를 반환하고 userId를 넘긴다")
    void allocate() throws Exception {
        when(useCase.allocate(any(StockAllocateCommand.class)))
                .thenReturn(new StockAllocateResult(3L, "SO-20261005-0001", List.of(summary())));

        mockMvc.perform(post("/api/v1/allocations").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"storeOrderId\": 3 }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderNo").value("SO-20261005-0001"))
                .andExpect(jsonPath("$.data.items[0].allocationId").value(500))
                .andExpect(jsonPath("$.data.items[0].status").value("ALLOCATED"))
                .andExpect(jsonPath("$.data.items[0].releasedAt").doesNotExist());

        ArgumentCaptor<StockAllocateCommand> captor = ArgumentCaptor.forClass(StockAllocateCommand.class);
        verify(useCase).allocate(captor.capture());
        assertThat(captor.getValue().storeOrderId()).isEqualTo(3L);
        assertThat(captor.getValue().userId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("할당 생성 발주 ID 누락은 400, 재고 부족은 409 INSUFFICIENT_STOCK")
    void allocateErrors() throws Exception {
        mockMvc.perform(post("/api/v1/allocations").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{ }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(useCase);

        when(useCase.allocate(any(StockAllocateCommand.class)))
                .thenThrow(new BusinessException(OutboundErrorCode.INSUFFICIENT_STOCK));
        mockMvc.perform(post("/api/v1/allocations").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"storeOrderId\": 3 }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @DisplayName("목록은 data.items와 필터를 전달하고 잘못된 status는 400")
    void list() throws Exception {
        when(useCase.searchAllocations(any(StockAllocationSearchCondition.class))).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/allocations").param("storeOrderId", "3").param("status", "ALLOCATED")
                        .param("keyword", "LOT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].allocationId").value(500));

        ArgumentCaptor<StockAllocationSearchCondition> captor =
                ArgumentCaptor.forClass(StockAllocationSearchCondition.class);
        verify(useCase).searchAllocations(captor.capture());
        assertThat(captor.getValue().storeOrderId()).isEqualTo(3L);
        assertThat(captor.getValue().status()).isEqualTo(AllocationStatus.ALLOCATED);
        assertThat(captor.getValue().keyword()).isEqualTo("LOT");

        mockMvc.perform(get("/api/v1/allocations").param("status", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("상세는 outboundId를 담고 allocatedByName은 null, 없는 할당은 404")
    void detail() throws Exception {
        StockAllocationView view = new StockAllocationView(500L, AllocationStatus.ALLOCATED, 3L, 0L, NOW, 9L,
                null, 3L, "SO-20261005-0001", 1L, "강남점", 2L, 31L, 5L, 5L, "SKU-A", "상품 A", 900L, 9L, "LOT-A",
                LocalDate.of(2026, 12, 31), 4L, "A-01", "A-01 구역");
        when(useCase.getAllocation(500L)).thenReturn(new StockAllocationDetail(view, 7L));
        when(useCase.getAllocation(999L)).thenThrow(new BusinessException(OutboundErrorCode.ALLOCATION_NOT_FOUND));

        mockMvc.perform(get("/api/v1/allocations/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outboundId").value(7))
                .andExpect(jsonPath("$.data.allocatedByName").doesNotExist())
                .andExpect(jsonPath("$.data.storeName").value("강남점"));
        mockMvc.perform(get("/api/v1/allocations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ALLOCATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("해제는 사유를 넘기고 inventory를 반환, 사유 누락은 400, 출고 연결은 409")
    void release() throws Exception {
        when(useCase.release(new StockAllocationReleaseCommand(500L, "사유", 9L))).thenReturn(
                new StockAllocationReleaseResult(500L, AllocationStatus.RELEASED, 3L, NOW, 900L, 50L, 0L, 50L));
        when(useCase.release(new StockAllocationReleaseCommand(501L, "사유", 9L)))
                .thenThrow(new BusinessException(OutboundErrorCode.ALLOCATION_IN_OUTBOUND));

        mockMvc.perform(patch("/api/v1/allocations/500/release").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"reason\": \"사유\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RELEASED"))
                .andExpect(jsonPath("$.data.inventory.availableQuantity").value(50));
        mockMvc.perform(patch("/api/v1/allocations/500/release").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{ }"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/allocations/501/release").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"reason\": \"사유\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALLOCATION_IN_OUTBOUND"));
    }
}

package com.kb.wms.outbound.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.kb.wms.outbound.application.port.in.OutboundFulfillmentUseCase;
import com.kb.wms.outbound.application.port.in.OutboundUseCase;
import com.kb.wms.outbound.application.port.in.command.OutboundCancelCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundCreateCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundPickingCompleteCommand;
import com.kb.wms.outbound.application.port.in.query.OutboundSearchCondition;
import com.kb.wms.outbound.application.port.in.result.OutboundCancelResult;
import com.kb.wms.outbound.application.port.in.result.OutboundCreateResult;
import com.kb.wms.outbound.application.port.in.result.OutboundDeliverResult;
import com.kb.wms.outbound.application.port.in.result.OutboundDetail;
import com.kb.wms.outbound.application.port.in.result.OutboundLineView;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingCompleteResult;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingCompleteResult.InventoryState;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingCompleteResult.PickedItem;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingStartResult;
import com.kb.wms.outbound.application.port.in.result.OutboundShipResult;
import com.kb.wms.outbound.application.port.in.result.OutboundSummary;
import com.kb.wms.outbound.application.port.in.result.OutboundView;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.outbound.exception.OutboundErrorCode;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

@WebMvcTest(OutboundController.class)
@AutoConfigureMockMvc(addFilters = false)
class OutboundControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 14, 0);

    @Autowired MockMvc mockMvc;
    @MockitoBean OutboundUseCase outboundUseCase;
    @MockitoBean OutboundFulfillmentUseCase fulfillmentUseCase;

    private OutboundView view(OutboundStatus status) {
        return new OutboundView(7L, "OB-20261005-0001", status, 3L, "SO-20261005-0001", 1L, "강남점", 2L,
                "서울 물류센터", null, null, null, "비고", NOW, NOW);
    }

    private OutboundLineView line() {
        return new OutboundLineView(70L, 500L, 31L, 5L, "SKU-A", "상품 A", "EA", 900L, 9L, "LOT-A",
                LocalDate.of(2026, 12, 31), 4L, "A-01", 3L, 0L, null);
    }

    // ---------- 생성·조회 ----------

    @Test
    @DisplayName("출고 생성은 201과 outboundNo·lineCount·items를 반환하고 userId를 넘긴다")
    void create() throws Exception {
        when(outboundUseCase.createOutbound(any(OutboundCreateCommand.class)))
                .thenReturn(new OutboundCreateResult(view(OutboundStatus.READY), List.of(line())));

        mockMvc.perform(post("/api/v1/outbounds").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"storeOrderId\": 3, \"note\": \"비고\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.outboundNo").value("OB-20261005-0001"))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.lineCount").value(1))
                .andExpect(jsonPath("$.data.items[0].outboundLineId").value(70))
                .andExpect(jsonPath("$.data.items[0].shippedQuantity").value(0));

        ArgumentCaptor<OutboundCreateCommand> captor = ArgumentCaptor.forClass(OutboundCreateCommand.class);
        verify(outboundUseCase).createOutbound(captor.capture());
        assertThat(captor.getValue().storeOrderId()).isEqualTo(3L);
        assertThat(captor.getValue().userId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("출고 생성 입력 검증: 발주 ID 누락, 비고 501자는 400 VALIDATION_ERROR")
    void createInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/outbounds").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{ }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/v1/outbounds").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"storeOrderId\": 3, \"note\": \"" + "가".repeat(501) + "\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(outboundUseCase);
    }

    @Test
    @DisplayName("목록은 data.items와 필터를 전달하고 잘못된 status는 400")
    void list() throws Exception {
        when(outboundUseCase.searchOutbounds(any(OutboundSearchCondition.class))).thenReturn(List.of(
                new OutboundSummary(7L, "OB-20261005-0001", 3L, "SO-20261005-0001", 1L, "강남점", 2L,
                        "서울 물류센터", OutboundStatus.READY, 2L, null, null, NOW)));

        mockMvc.perform(get("/api/v1/outbounds").param("status", "READY").param("storeOrderId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].outboundNo").value("OB-20261005-0001"))
                .andExpect(jsonPath("$.data.items[0].lineCount").value(2))
                .andExpect(jsonPath("$.data.items[0].shippedAt").doesNotExist());

        ArgumentCaptor<OutboundSearchCondition> captor = ArgumentCaptor.forClass(OutboundSearchCondition.class);
        verify(outboundUseCase).searchOutbounds(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(OutboundStatus.READY);
        assertThat(captor.getValue().storeOrderId()).isEqualTo(3L);

        mockMvc.perform(get("/api/v1/outbounds").param("status", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("상세는 항목과 lineAmount를 담고 없는 출고는 404 OUTBOUND_NOT_FOUND")
    void details() throws Exception {
        OutboundLineView picked = new OutboundLineView(70L, 500L, 31L, 5L, "SKU-A", "상품 A", "EA", 900L, 9L,
                "LOT-A", LocalDate.of(2026, 12, 31), 4L, "A-01", 3L, 2L, new BigDecimal("1000"));
        when(outboundUseCase.getOutbound(7L)).thenReturn(
                new OutboundDetail(view(OutboundStatus.PICKED), List.of(picked), null));
        when(outboundUseCase.getOutbound(999L)).thenThrow(new BusinessException(OutboundErrorCode.OUTBOUND_NOT_FOUND));

        mockMvc.perform(get("/api/v1/outbounds/7/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].confirmedUnitSupplyPrice").value(1000))
                .andExpect(jsonPath("$.data.items[0].lineAmount").value(2000))
                .andExpect(jsonPath("$.data.cancelReason").doesNotExist());
        mockMvc.perform(get("/api/v1/outbounds/999/details"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("OUTBOUND_NOT_FOUND"));
    }

    // ---------- 상태 전이 ----------

    @Test
    @DisplayName("피킹 시작은 userId를 넘기고 PICKING을 반환, 상태 충돌은 409")
    void startPicking() throws Exception {
        when(outboundUseCase.startPicking(7L, 9L)).thenReturn(
                new OutboundPickingStartResult(7L, "OB-20261005-0001", OutboundStatus.PICKING, NOW));
        when(outboundUseCase.startPicking(8L, 9L)).thenThrow(new BusinessException(OutboundErrorCode.ORDER_NOT_ASSIGNED));

        mockMvc.perform(patch("/api/v1/outbounds/7/picking/start").param("userId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PICKING"));
        mockMvc.perform(patch("/api/v1/outbounds/8/picking/start").param("userId", "9"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_ASSIGNED"));
        mockMvc.perform(patch("/api/v1/outbounds/7/picking/start"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("피킹 완료는 lines를 명령으로 바꾸고 shortage·inventory를 반환한다")
    void completePicking() throws Exception {
        PickedItem item = new PickedItem(70L, 500L, 3L, 2L, new BigDecimal("1000"),
                new InventoryState(900L, 47L, 0L));
        when(fulfillmentUseCase.completePicking(any(OutboundPickingCompleteCommand.class))).thenReturn(
                new OutboundPickingCompleteResult(7L, "OB-20261005-0001", OutboundStatus.PICKED, true,
                        List.of(item), NOW));

        mockMvc.perform(patch("/api/v1/outbounds/7/picking/complete").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"lines\": [ { \"outboundLineId\": 70, \"pickedQuantity\": 2 } ] }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PICKED"))
                .andExpect(jsonPath("$.data.hasShortage").value(true))
                .andExpect(jsonPath("$.data.items[0].shortageQuantity").value(1))
                .andExpect(jsonPath("$.data.items[0].lineAmount").value(2000))
                .andExpect(jsonPath("$.data.items[0].inventory.onHandQuantity").value(47));

        ArgumentCaptor<OutboundPickingCompleteCommand> captor =
                ArgumentCaptor.forClass(OutboundPickingCompleteCommand.class);
        verify(fulfillmentUseCase).completePicking(captor.capture());
        assertThat(captor.getValue().outboundId()).isEqualTo(7L);
        assertThat(captor.getValue().userId()).isEqualTo(9L);
        assertThat(captor.getValue().lines()).hasSize(1);
        assertThat(captor.getValue().lines().get(0).pickedQuantity()).isEqualTo(2L);
    }

    @Test
    @DisplayName("피킹 완료 입력 검증: lines 누락·빈 배열·음수 수량·ID 누락은 400")
    void completePickingInvalid() throws Exception {
        for (String body : List.of("{ }", "{ \"lines\": [] }",
                "{ \"lines\": [ { \"outboundLineId\": 70, \"pickedQuantity\": -1 } ] }",
                "{ \"lines\": [ { \"pickedQuantity\": 1 } ] }",
                "{ \"lines\": [ { \"outboundLineId\": 70 } ] }")) {
            mockMvc.perform(patch("/api/v1/outbounds/7/picking/complete").param("userId", "9")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(fulfillmentUseCase);
    }

    @Test
    @DisplayName("피킹 완료 NOTHING_PICKED는 409")
    void completePickingNothingPicked() throws Exception {
        when(fulfillmentUseCase.completePicking(any(OutboundPickingCompleteCommand.class)))
                .thenThrow(new BusinessException(OutboundErrorCode.NOTHING_PICKED));

        mockMvc.perform(patch("/api/v1/outbounds/7/picking/complete").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"lines\": [ { \"outboundLineId\": 70, \"pickedQuantity\": 0 } ] }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOTHING_PICKED"));
    }

    @Test
    @DisplayName("배송 시작은 shippedAt·shippedBy를, 배송 완료는 storeOrder 상태를 반환한다")
    void shipAndDeliver() throws Exception {
        when(fulfillmentUseCase.ship(7L, 9L)).thenReturn(
                new OutboundShipResult(7L, "OB-20261005-0001", OutboundStatus.SHIPPED, NOW, 9L, NOW));
        when(fulfillmentUseCase.deliver(7L, 9L)).thenReturn(
                new OutboundDeliverResult(7L, "OB-20261005-0001", OutboundStatus.DELIVERED, NOW, 3L,
                        StoreOrderStatus.COMPLETED, NOW));

        mockMvc.perform(patch("/api/v1/outbounds/7/ship").param("userId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"))
                .andExpect(jsonPath("$.data.shippedBy").value(9));
        mockMvc.perform(patch("/api/v1/outbounds/7/deliver").param("userId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.storeOrder.storeOrderId").value(3))
                .andExpect(jsonPath("$.data.storeOrder.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("출고 취소는 사유를 넘기고 사유 누락·501자는 400")
    void cancel() throws Exception {
        when(outboundUseCase.cancel(any(OutboundCancelCommand.class))).thenReturn(
                new OutboundCancelResult(7L, "OB-20261005-0001", OutboundStatus.CANCELED, "오배정", NOW));

        mockMvc.perform(patch("/api/v1/outbounds/7/cancel").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"reason\": \"오배정\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.cancelReason").value("오배정"));
        verify(outboundUseCase).cancel(eq(new OutboundCancelCommand(7L, "오배정", 9L)));

        mockMvc.perform(patch("/api/v1/outbounds/7/cancel").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"reason\": \" \" }"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/outbounds/7/cancel").param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"" + "가".repeat(501) + "\" }"))
                .andExpect(status().isBadRequest());
    }
}

package com.kb.wms.inbound.adapter.in.web;

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
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.application.port.in.InboundCompleteUseCase;
import com.kb.wms.inbound.application.port.in.InboundInspectUseCase;
import com.kb.wms.inbound.application.port.in.InboundUseCase;
import com.kb.wms.inbound.application.port.in.command.InboundCancelCommand;
import com.kb.wms.inbound.application.port.in.command.InboundInspectCommand;
import com.kb.wms.inbound.application.port.in.command.InboundRegisterCommand;
import com.kb.wms.inbound.application.port.in.query.InboundSearchCondition;
import com.kb.wms.inbound.application.port.in.query.SectionCandidateCondition;
import com.kb.wms.inbound.application.port.in.result.InboundCompleteResult;
import com.kb.wms.inbound.application.port.in.result.InboundDetails;
import com.kb.wms.inbound.application.port.in.result.InboundLineView;
import com.kb.wms.inbound.application.port.in.result.InboundSummary;
import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.application.port.in.result.SectionCandidate;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.exception.InboundErrorCode;
import com.kb.wms.inventory.exception.InventoryErrorCode;

@WebMvcTest(InboundController.class)
@AutoConfigureMockMvc(addFilters = false)
class InboundControllerTest {

    private static final String INSPECT_BODY = """
            {
              "lines": [
                {
                  "purchaseOrderLineId": 11,
                  "lotNumber": "LOT-20260901-A",
                  "manufacturedDate": "2026-08-20",
                  "expiryDate": null,
                  "receivedQuantity": 60,
                  "acceptedQuantity": 58,
                  "defectiveQuantity": 2,
                  "receivedUnitPrice": 60000.00,
                  "inspectionNote": "포장 파손 2개",
                  "acceptedSectionId": 2,
                  "defectSectionId": 9
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InboundUseCase inboundUseCase;

    @MockitoBean
    private InboundInspectUseCase inboundInspectUseCase;

    @MockitoBean
    private InboundCompleteUseCase inboundCompleteUseCase;

    // ---------- fixtures ----------

    private static InboundView view(InboundStatus status) {
        return new InboundView(
                7L, "IB-20260925-0001", 4L, "PO-20260921-0001", PurchaseOrderStatus.CONFIRMED, 3L, "공급처 A",
                1L, "서울 물류센터", status, LocalDateTime.of(2026, 9, 25, 9, 10),
                null, null, null, "1차 입고", 1L, LocalDateTime.of(2026, 9, 25, 9, 12),
                LocalDateTime.of(2026, 9, 25, 11, 0));
    }

    private static InboundSummary summary() {
        return new InboundSummary(
                7L, "IB-20260925-0001", 4L, "PO-20260921-0001", 3L, "공급처 A", 1L, "서울 물류센터",
                InboundStatus.INSPECTING, LocalDateTime.of(2026, 9, 25, 9, 10), null, null, 1L);
    }

    private static InboundLineView lineView() {
        return new InboundLineView(
                21L, 11L, 1L, "SKU-0001-RED-G4", "배드민턴 라켓 A 빨강 G4", 31L, "LOT-20260901-A",
                LocalDate.of(2026, 8, 20), null, 60L, 58L, 2L, 2L, "A-01-R01", 9L, "D-01",
                BigDecimal.valueOf(60000), BigDecimal.valueOf(60000), BigDecimal.valueOf(3600000),
                null, "포장 파손 2개", LocalDateTime.of(2026, 9, 25, 11, 0), 5L);
    }

    private static Inbound inbound(InboundStatus status) {
        return Inbound.builder()
                .inboundId(7L)
                .inboundNo("IB-20260925-0001")
                .purchaseOrderId(4L)
                .warehouseId(1L)
                .status(status)
                .receivedAt(status == InboundStatus.COMPLETED ? LocalDateTime.of(2026, 9, 25, 11, 30) : null)
                .receivedBy(status == InboundStatus.COMPLETED ? 5L : null)
                .updatedAt(LocalDateTime.of(2026, 9, 25, 11, 30))
                .build();
    }

    private static SectionCandidate candidate(Long id, String type) {
        return new SectionCandidate(
                id, null, "A-01-R01", "A구역 1번 랙", type,
                BigDecimal.valueOf(800), BigDecimal.valueOf(120), BigDecimal.valueOf(680));
    }

    // ---------- 등록 ----------

    @Test
    @DisplayName("입고를 등록하면 201과 명세의 등록 응답(헤더)을 반환한다")
    void register_success() throws Exception {
        when(inboundUseCase.registerInbound(any(InboundRegisterCommand.class))).thenReturn(7L);
        when(inboundUseCase.getInbound(7L)).thenReturn(view(InboundStatus.ARRIVED));

        mockMvc.perform(post("/api/v1/inbounds")
                        .param("userId", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "purchaseOrderId": 4, "arrivedAt": "2026-09-25T09:10:00", "note": "1차 입고" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.inboundId").value(7))
                .andExpect(jsonPath("$.data.inboundNo").value("IB-20260925-0001"))
                .andExpect(jsonPath("$.data.purchaseOrderId").value(4))
                .andExpect(jsonPath("$.data.purchaseOrderNo").value("PO-20260921-0001"))
                .andExpect(jsonPath("$.data.warehouseId").value(1))
                .andExpect(jsonPath("$.data.warehouseName").value("서울 물류센터"))
                .andExpect(jsonPath("$.data.status").value("ARRIVED"))
                .andExpect(jsonPath("$.data.note").value("1차 입고"));

        ArgumentCaptor<InboundRegisterCommand> captor = ArgumentCaptor.forClass(InboundRegisterCommand.class);
        verify(inboundUseCase).registerInbound(captor.capture());
        assertThat(captor.getValue().purchaseOrderId()).isEqualTo(4L);
        assertThat(captor.getValue().arrivedAt()).isEqualTo(LocalDateTime.of(2026, 9, 25, 9, 10));
        assertThat(captor.getValue().note()).isEqualTo("1차 입고");
    }

    @Test
    @DisplayName("발주 ID가 없거나 비고가 1000자를 넘으면 400 VALIDATION_ERROR이고 서비스를 호출하지 않는다")
    void register_validation() throws Exception {
        mockMvc.perform(post("/api/v1/inbounds")
                        .param("userId", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"note\": \"비고\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/inbounds")
                        .param("userId", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"purchaseOrderId\": 4, \"note\": \"" + "가".repeat(1001) + "\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(inboundUseCase);
    }

    @Test
    @DisplayName("같은 발주에 진행 중인 입고가 있으면 409 INBOUND_IN_PROGRESS를 반환한다")
    void register_inProgress() throws Exception {
        when(inboundUseCase.registerInbound(any(InboundRegisterCommand.class)))
                .thenThrow(new BusinessException(InboundErrorCode.INBOUND_IN_PROGRESS));

        mockMvc.perform(post("/api/v1/inbounds")
                        .param("userId", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"purchaseOrderId\": 4 }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INBOUND_IN_PROGRESS"));
    }

    // ---------- 목록·단건·상세 ----------

    @Test
    @DisplayName("입고 목록은 data.items로 반환하고 필터를 조회 조건으로 넘긴다")
    void list_success() throws Exception {
        when(inboundUseCase.getInbounds(any(InboundSearchCondition.class))).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/inbounds")
                        .param("status", "INSPECTING")
                        .param("warehouseId", "1")
                        .param("purchaseOrderId", "4")
                        .param("keyword", "IB-")
                        .param("arrivedFrom", "2026-09-01T00:00:00")
                        .param("arrivedTo", "2026-09-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].inboundId").value(7))
                .andExpect(jsonPath("$.data.items[0].supplierName").value("공급처 A"))
                .andExpect(jsonPath("$.data.items[0].status").value("INSPECTING"))
                .andExpect(jsonPath("$.data.items[0].lineCount").value(1));

        ArgumentCaptor<InboundSearchCondition> captor = ArgumentCaptor.forClass(InboundSearchCondition.class);
        verify(inboundUseCase).getInbounds(captor.capture());
        InboundSearchCondition condition = captor.getValue();
        assertThat(condition.status()).isEqualTo(InboundStatus.INSPECTING);
        assertThat(condition.warehouseId()).isEqualTo(1L);
        assertThat(condition.purchaseOrderId()).isEqualTo(4L);
        assertThat(condition.keyword()).isEqualTo("IB-");
        assertThat(condition.arrivedFrom()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
        assertThat(condition.arrivedTo()).isEqualTo(LocalDateTime.of(2026, 9, 30, 23, 59, 59));
    }

    @Test
    @DisplayName("입고 목록의 상태 값이 올바르지 않으면 400을 반환한다")
    void list_invalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/inbounds").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inboundUseCase);
    }

    @Test
    @DisplayName("입고 단건을 조회하면 명세의 단건 응답을 반환한다")
    void get_success() throws Exception {
        when(inboundUseCase.getInbound(7L)).thenReturn(view(InboundStatus.INSPECTING));

        mockMvc.perform(get("/api/v1/inbounds/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inboundId").value(7))
                .andExpect(jsonPath("$.data.supplierId").value(3))
                .andExpect(jsonPath("$.data.status").value("INSPECTING"))
                .andExpect(jsonPath("$.data.lineCount").value(1))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    @DisplayName("없는 입고를 조회하면 404 INBOUND_NOT_FOUND를 반환한다")
    void get_notFound() throws Exception {
        when(inboundUseCase.getInbound(999L)).thenThrow(new BusinessException(InboundErrorCode.INBOUND_NOT_FOUND));

        mockMvc.perform(get("/api/v1/inbounds/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INBOUND_NOT_FOUND"));
    }

    @Test
    @DisplayName("입고 ID가 숫자가 아니면 400을 반환한다")
    void get_invalidId() throws Exception {
        mockMvc.perform(get("/api/v1/inbounds/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inboundUseCase);
    }

    @Test
    @DisplayName("입고 상세는 검수 항목을 items로 반환한다")
    void details_success() throws Exception {
        when(inboundUseCase.getInboundDetails(7L)).thenReturn(new InboundDetails(
                7L, "IB-20260925-0001", InboundStatus.INSPECTING, List.of(lineView())));

        mockMvc.perform(get("/api/v1/inbounds/7/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inboundNo").value("IB-20260925-0001"))
                .andExpect(jsonPath("$.data.items[0].inboundLineId").value(21))
                .andExpect(jsonPath("$.data.items[0].skuCode").value("SKU-0001-RED-G4"))
                .andExpect(jsonPath("$.data.items[0].lotNumber").value("LOT-20260901-A"))
                .andExpect(jsonPath("$.data.items[0].manufacturedDate").value("2026-08-20"))
                .andExpect(jsonPath("$.data.items[0].receivedQuantity").value(60))
                .andExpect(jsonPath("$.data.items[0].acceptedSectionCode").value("A-01-R01"))
                .andExpect(jsonPath("$.data.items[0].defectSectionCode").value("D-01"))
                .andExpect(jsonPath("$.data.items[0].lineAmount").value(3600000.0));
    }

    // ---------- 검수 ----------

    @Test
    @DisplayName("검수하면 요청 항목을 명령으로 넘기고 저장된 검수 항목과 상태를 반환한다")
    void inspect_success() throws Exception {
        when(inboundInspectUseCase.inspectInbound(eq(7L), any(InboundInspectCommand.class)))
                .thenReturn(inbound(InboundStatus.INSPECTING));
        when(inboundUseCase.getInboundDetails(7L)).thenReturn(new InboundDetails(
                7L, "IB-20260925-0001", InboundStatus.INSPECTING, List.of(lineView())));

        mockMvc.perform(patch("/api/v1/inbounds/7/inspect")
                        .param("userId", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSPECT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inboundId").value(7))
                .andExpect(jsonPath("$.data.status").value("INSPECTING"))
                .andExpect(jsonPath("$.data.items[0].inboundLineId").value(21))
                .andExpect(jsonPath("$.data.items[0].lotId").value(31))
                .andExpect(jsonPath("$.data.items[0].acceptedSectionId").value(2))
                .andExpect(jsonPath("$.data.items[0].defectSectionId").value(9))
                .andExpect(jsonPath("$.data.items[0].orderedUnitPrice").value(60000.0))
                .andExpect(jsonPath("$.data.items[0].lineAmount").value(3600000.0))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        ArgumentCaptor<InboundInspectCommand> captor = ArgumentCaptor.forClass(InboundInspectCommand.class);
        verify(inboundInspectUseCase).inspectInbound(eq(7L), captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(5L);
        assertThat(captor.getValue().lines()).hasSize(1);
        InboundInspectCommand.Line line = captor.getValue().lines().get(0);
        assertThat(line.purchaseOrderLineId()).isEqualTo(11L);
        assertThat(line.lotNumber()).isEqualTo("LOT-20260901-A");
        assertThat(line.manufacturedDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(line.expiryDate()).isNull();
        assertThat(line.receivedQuantity()).isEqualTo(60L);
        assertThat(line.acceptedQuantity()).isEqualTo(58L);
        assertThat(line.defectiveQuantity()).isEqualTo(2L);
        assertThat(line.receivedUnitPrice()).isEqualByComparingTo("60000");
        assertThat(line.inspectionNote()).isEqualTo("포장 파손 2개");
        assertThat(line.acceptedSectionId()).isEqualTo(2L);
        assertThat(line.defectSectionId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("검수 요청이 잘못되면 400 VALIDATION_ERROR이고 서비스를 호출하지 않는다")
    void inspect_validation() throws Exception {
        List<String> invalidBodies = List.of(
                "{ }",
                "{ \"lines\": [] }",
                lineBody("\"lotNumber\": \"\""),
                lineBody("\"lotNumber\": \"" + "L".repeat(101) + "\""),
                lineBody("\"receivedQuantity\": 0"),
                lineBody("\"acceptedQuantity\": -1"),
                lineBody("\"defectiveQuantity\": -1"),
                lineBody("\"receivedUnitPrice\": -1"),
                lineBody("\"receivedUnitPrice\": 100.123"),
                lineBody("\"priceChangeReason\": \"" + "가".repeat(501) + "\""),
                lineBody("\"inspectionNote\": \"" + "가".repeat(1001) + "\""),
                lineBody("\"purchaseOrderLineId\": null"));

        for (String body : invalidBodies) {
            mockMvc.perform(patch("/api/v1/inbounds/7/inspect")
                            .param("userId", "5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(inboundInspectUseCase);
    }

    /** 유효한 검수 항목에서 한 필드만 덮어쓴 요청 바디. JSON은 마지막 중복 키 값을 쓴다. */
    private static String lineBody(String overriddenField) {
        return """
                {
                  "lines": [
                    {
                      "purchaseOrderLineId": 11,
                      "lotNumber": "LOT-A",
                      "receivedQuantity": 60,
                      "acceptedQuantity": 58,
                      "defectiveQuantity": 2,
                      "receivedUnitPrice": 60000,
                      %s
                    }
                  ]
                }
                """.formatted(overriddenField);
    }

    @Test
    @DisplayName("처리 사용자 파라미터가 없으면 400을 반환한다")
    void inspect_missingUserId() throws Exception {
        mockMvc.perform(patch("/api/v1/inbounds/7/inspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSPECT_BODY))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inboundInspectUseCase);
    }

    @Test
    @DisplayName("로트 원가가 기존 로트와 다르면 409 LOT_UNIT_COST_MISMATCH를 반환한다")
    void inspect_lotConflict() throws Exception {
        when(inboundInspectUseCase.inspectInbound(eq(7L), any(InboundInspectCommand.class)))
                .thenThrow(new BusinessException(InventoryErrorCode.LOT_UNIT_COST_MISMATCH));

        mockMvc.perform(patch("/api/v1/inbounds/7/inspect")
                        .param("userId", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSPECT_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LOT_UNIT_COST_MISMATCH"));
    }

    // ---------- 완료 ----------

    @Test
    @DisplayName("입고를 완료하면 반영된 재고와 발주·발주 항목 상태를 반환한다")
    void complete_success() throws Exception {
        when(inboundCompleteUseCase.completeInbound(7L, 5L)).thenReturn(new InboundCompleteResult(
                inbound(InboundStatus.COMPLETED),
                List.of(new InboundCompleteResult.ReflectedInventory(21L, 101L, 58L, 105L, 2L)),
                4L, PurchaseOrderStatus.CONFIRMED,
                List.of(new InboundCompleteResult.PurchaseOrderLineProgress(
                        11L, 100L, 60L, PurchaseOrderLineStatus.PARTIALLY_RECEIVED))));

        mockMvc.perform(patch("/api/v1/inbounds/7/complete").param("userId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inboundId").value(7))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.receivedBy").value(5))
                .andExpect(jsonPath("$.data.receivedAt").exists())
                .andExpect(jsonPath("$.data.inventory[0].inboundLineId").value(21))
                .andExpect(jsonPath("$.data.inventory[0].acceptedInventoryLotId").value(101))
                .andExpect(jsonPath("$.data.inventory[0].acceptedQuantity").value(58))
                .andExpect(jsonPath("$.data.inventory[0].defectiveInventoryLotId").value(105))
                .andExpect(jsonPath("$.data.inventory[0].defectiveQuantity").value(2))
                .andExpect(jsonPath("$.data.purchaseOrder.purchaseOrderId").value(4))
                .andExpect(jsonPath("$.data.purchaseOrder.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.purchaseOrderLines[0].purchaseOrderLineId").value(11))
                .andExpect(jsonPath("$.data.purchaseOrderLines[0].expectedQuantity").value(100))
                .andExpect(jsonPath("$.data.purchaseOrderLines[0].receivedQuantity").value(60))
                .andExpect(jsonPath("$.data.purchaseOrderLines[0].status").value("PARTIALLY_RECEIVED"));
    }

    @Test
    @DisplayName("구역이 지정되지 않았으면 409 SECTION_NOT_ASSIGNED를 반환한다")
    void complete_sectionNotAssigned() throws Exception {
        when(inboundCompleteUseCase.completeInbound(7L, 5L))
                .thenThrow(new BusinessException(InboundErrorCode.SECTION_NOT_ASSIGNED));

        mockMvc.perform(patch("/api/v1/inbounds/7/complete").param("userId", "5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SECTION_NOT_ASSIGNED"));
    }

    @Test
    @DisplayName("처리 사용자 파라미터가 없으면 완료 요청은 400을 반환한다")
    void complete_missingUserId() throws Exception {
        mockMvc.perform(patch("/api/v1/inbounds/7/complete"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inboundCompleteUseCase);
    }

    // ---------- 취소 ----------

    @Test
    @DisplayName("입고를 취소하면 취소 상태와 발주 상태를 반환한다")
    void cancel_success() throws Exception {
        when(inboundUseCase.cancelInbound(eq(7L), any(InboundCancelCommand.class)))
                .thenReturn(inbound(InboundStatus.CANCELED));
        when(inboundUseCase.getInbound(7L))
                .thenReturn(view(InboundStatus.CANCELED).withCancelReason("발주와 다른 상품이 도착해 전량 반송"));

        mockMvc.perform(patch("/api/v1/inbounds/7/cancel")
                        .param("userId", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"발주와 다른 상품이 도착해 전량 반송\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inboundId").value(7))
                .andExpect(jsonPath("$.data.inboundNo").value("IB-20260925-0001"))
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.purchaseOrder.purchaseOrderId").value(4))
                .andExpect(jsonPath("$.data.purchaseOrder.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.cancelReason").value("발주와 다른 상품이 도착해 전량 반송"))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        ArgumentCaptor<InboundCancelCommand> captor = ArgumentCaptor.forClass(InboundCancelCommand.class);
        verify(inboundUseCase).cancelInbound(eq(7L), captor.capture());
        assertThat(captor.getValue().reason()).isEqualTo("발주와 다른 상품이 도착해 전량 반송");
        assertThat(captor.getValue().userId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("취소 처리 사용자 파라미터가 없으면 400을 반환하고 유스케이스를 호출하지 않는다")
    void cancel_missingUserId() throws Exception {
        mockMvc.perform(patch("/api/v1/inbounds/7/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"사유\" }"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inboundUseCase);
    }

    @Test
    @DisplayName("등록 처리 사용자 파라미터가 없으면 400을 반환하고 유스케이스를 호출하지 않는다")
    void register_missingUserId() throws Exception {
        mockMvc.perform(post("/api/v1/inbounds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"purchaseOrderId\": 4 }"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inboundUseCase);
    }

    @Test
    @DisplayName("취소된 입고 단건을 조회하면 cancelReason을 반환한다")
    void getInbound_canceled_returnsCancelReason() throws Exception {
        when(inboundUseCase.getInbound(7L))
                .thenReturn(view(InboundStatus.CANCELED).withCancelReason("잘못된 발주에 등록"));

        mockMvc.perform(get("/api/v1/inbounds/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.cancelReason").value("잘못된 발주에 등록"));
    }

    @Test
    @DisplayName("취소 사유가 없거나 공백이거나 500자를 넘으면 400 VALIDATION_ERROR이고 서비스를 호출하지 않는다")
    void cancel_validation() throws Exception {
        for (String body : List.of("{ }", "{ \"reason\": \"  \" }",
                "{ \"reason\": \"" + "가".repeat(501) + "\" }")) {
            mockMvc.perform(patch("/api/v1/inbounds/7/cancel")
                        .param("userId", "5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(inboundUseCase);
    }

    @Test
    @DisplayName("완료된 입고를 취소하면 409 CONFLICT를 반환한다")
    void cancel_conflict() throws Exception {
        when(inboundUseCase.cancelInbound(eq(7L), any(InboundCancelCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "완료된 입고는 취소할 수 없습니다."));

        mockMvc.perform(patch("/api/v1/inbounds/7/cancel")
                        .param("userId", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"사유\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    // ---------- 구역 후보 ----------

    @Test
    @DisplayName("합격 구역 후보는 입고 ID·창고 ID와 items를 반환하고 조건을 넘긴다")
    void assignableSections_success() throws Exception {
        when(inboundUseCase.getInbound(7L)).thenReturn(view(InboundStatus.INSPECTING));
        when(inboundUseCase.getAssignableSections(eq(7L), any(SectionCandidateCondition.class)))
                .thenReturn(List.of(candidate(2L, "RACK")));

        mockMvc.perform(get("/api/v1/inbounds/7/assignable-sections")
                        .param("requiredQuantity", "58")
                        .param("keyword", "A-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inboundId").value(7))
                .andExpect(jsonPath("$.data.warehouseId").value(1))
                .andExpect(jsonPath("$.data.items[0].sectionId").value(2))
                .andExpect(jsonPath("$.data.items[0].sectionCode").value("A-01-R01"))
                .andExpect(jsonPath("$.data.items[0].sectionType").value("RACK"))
                .andExpect(jsonPath("$.data.items[0].capacity").value(800))
                .andExpect(jsonPath("$.data.items[0].currentCapacity").value(120))
                .andExpect(jsonPath("$.data.items[0].availableCapacity").value(680));

        ArgumentCaptor<SectionCandidateCondition> captor = ArgumentCaptor.forClass(SectionCandidateCondition.class);
        verify(inboundUseCase).getAssignableSections(eq(7L), captor.capture());
        assertThat(captor.getValue().requiredQuantity()).isEqualByComparingTo("58");
        assertThat(captor.getValue().keyword()).isEqualTo("A-01");
    }

    @Test
    @DisplayName("불량 구역 후보도 같은 응답 형식으로 반환한다")
    void defectSections_success() throws Exception {
        when(inboundUseCase.getInbound(7L)).thenReturn(view(InboundStatus.INSPECTING));
        when(inboundUseCase.getDefectSections(eq(7L), any(SectionCandidateCondition.class)))
                .thenReturn(List.of(candidate(9L, "DEFECT")));

        mockMvc.perform(get("/api/v1/inbounds/7/defect-sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inboundId").value(7))
                .andExpect(jsonPath("$.data.warehouseId").value(1))
                .andExpect(jsonPath("$.data.items[0].sectionId").value(9))
                .andExpect(jsonPath("$.data.items[0].sectionType").value("DEFECT"));
    }

    @Test
    @DisplayName("없는 입고의 구역 후보를 조회하면 404 INBOUND_NOT_FOUND를 반환한다")
    void sections_notFound() throws Exception {
        when(inboundUseCase.getInbound(999L)).thenThrow(new BusinessException(InboundErrorCode.INBOUND_NOT_FOUND));

        mockMvc.perform(get("/api/v1/inbounds/999/assignable-sections"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INBOUND_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/inbounds/999/defect-sections"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INBOUND_NOT_FOUND"));
    }
}

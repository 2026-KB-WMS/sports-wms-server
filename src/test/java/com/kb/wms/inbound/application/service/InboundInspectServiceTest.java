package com.kb.wms.inbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.inbound.application.port.in.command.InboundInspectCommand;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.InboundSectionPort;
import com.kb.wms.inbound.application.port.out.InboundSectionPort.SectionInfo;
import com.kb.wms.inbound.application.port.out.LotPort;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.InboundLine;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.exception.InboundErrorCode;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inventory.exception.InventoryErrorCode;

@ExtendWith(MockitoExtension.class)
class InboundInspectServiceTest {

    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    private static final long USER = 5L;
    private static final BigDecimal ORDERED_PRICE = BigDecimal.valueOf(60000);

    @Mock
    private InboundRepository inboundRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private LotPort lotPort;

    @Mock
    private InboundSectionPort inboundSectionPort;

    @Mock
    private StatusHistoryUseCase statusHistoryUseCase;

    @InjectMocks
    private InboundInspectService inboundInspectService;

    // ---------- fixtures ----------

    private static Inbound inbound(InboundStatus status) {
        return Inbound.builder()
                .inboundId(7L)
                .inboundNo("IB-20261002-0001")
                .purchaseOrderId(4L)
                .warehouseId(1L)
                .status(status)
                .build();
    }

    private static PurchaseOrder purchaseOrder() {
        return PurchaseOrder.builder()
                .purchaseOrderId(4L)
                .purchaseOrderNo("PO-20261002-0001")
                .warehouseId(1L)
                .supplierId(3L)
                .status(PurchaseOrderStatus.CONFIRMED)
                .build();
    }

    private static PurchaseOrderLine purchaseOrderLine(Long id, Long purchaseOrderId, long expected, long received) {
        return PurchaseOrderLine.builder()
                .purchaseOrderLineId(id)
                .purchaseOrderId(purchaseOrderId)
                .skuId(1L)
                .expectedQuantity(expected)
                .receivedQuantity(received)
                .orderedUnitPrice(ORDERED_PRICE)
                .lineAmount(ORDERED_PRICE.multiply(BigDecimal.valueOf(expected)))
                .build();
    }

    private static InboundInspectCommand.Line line(long received, long accepted, long defective) {
        return new InboundInspectCommand.Line(
                11L, "LOT-A", LocalDate.of(2026, 8, 20), null, received, accepted, defective,
                ORDERED_PRICE, null, "포장 파손", 2L, 9L);
    }

    private static InboundInspectCommand.Line line(String lotNumber, long received) {
        return new InboundInspectCommand.Line(
                11L, lotNumber, null, null, received, received, 0L, ORDERED_PRICE, null, null, 2L, null);
    }

    private static InboundInspectCommand command(InboundInspectCommand.Line... lines) {
        return new InboundInspectCommand(USER, List.of(lines));
    }

    private static InboundInspectCommand.Line withLot(InboundInspectCommand.Line source, String lotNumber,
                                                      LocalDate manufacturedDate, LocalDate expiryDate) {
        return new InboundInspectCommand.Line(
                source.purchaseOrderLineId(), lotNumber, manufacturedDate, expiryDate, source.receivedQuantity(),
                source.acceptedQuantity(), source.defectiveQuantity(), source.receivedUnitPrice(),
                source.priceChangeReason(), source.inspectionNote(), source.acceptedSectionId(),
                source.defectSectionId());
    }

    private static InboundInspectCommand.Line withPrice(InboundInspectCommand.Line source, BigDecimal price,
                                                        String reason) {
        return new InboundInspectCommand.Line(
                source.purchaseOrderLineId(), source.lotNumber(), source.manufacturedDate(), source.expiryDate(),
                source.receivedQuantity(), source.acceptedQuantity(), source.defectiveQuantity(), price, reason,
                source.inspectionNote(), source.acceptedSectionId(), source.defectSectionId());
    }

    private static InboundInspectCommand.Line withSections(InboundInspectCommand.Line source, Long acceptedSectionId,
                                                           Long defectSectionId) {
        return new InboundInspectCommand.Line(
                source.purchaseOrderLineId(), source.lotNumber(), source.manufacturedDate(), source.expiryDate(),
                source.receivedQuantity(), source.acceptedQuantity(), source.defectiveQuantity(),
                source.receivedUnitPrice(), source.priceChangeReason(), source.inspectionNote(),
                acceptedSectionId, defectSectionId);
    }

    private static InboundInspectCommand.Line withNotes(InboundInspectCommand.Line source, String reason, String note) {
        return new InboundInspectCommand.Line(
                source.purchaseOrderLineId(), source.lotNumber(), source.manufacturedDate(), source.expiryDate(),
                source.receivedQuantity(), source.acceptedQuantity(), source.defectiveQuantity(),
                source.receivedUnitPrice(), reason, note, source.acceptedSectionId(), source.defectSectionId());
    }

    private static SectionInfo rack(Long id, Long warehouseId, boolean active) {
        return new SectionInfo(id, warehouseId, "RACK", active);
    }

    private static SectionInfo defect(Long id, Long warehouseId, boolean active) {
        return new SectionInfo(id, warehouseId, "DEFECT", active);
    }

    private static void assertError(ThrowingCallable call, String errorCodeName) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(errorCodeName);
    }

    private void givenInbound(InboundStatus status) {
        when(inboundRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(inbound(status)));
        when(purchaseOrderRepository.findById(4L)).thenReturn(Optional.of(purchaseOrder()));
    }

    private void givenPurchaseOrderLine(long expected, long received) {
        when(purchaseOrderRepository.findLineById(11L))
                .thenReturn(Optional.of(purchaseOrderLine(11L, 4L, expected, received)));
    }

    private void givenHappySections() {
        when(inboundSectionPort.getSection(2L)).thenReturn(rack(2L, 1L, true));
        when(inboundSectionPort.getSection(9L)).thenReturn(defect(9L, 1L, true));
    }

    private void stubSave() {
        when(inboundRepository.save(any(Inbound.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------- 성공 ----------

    @Test
    @DisplayName("첫 검수는 INSPECTING으로 바꾸고 기존 항목을 지운 뒤 로트를 찾거나 만들어 검수 항목을 저장한다")
    void inspect_firstCall() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 0);
        givenHappySections();
        when(lotPort.findOrRegisterLot(1L, 3L, "LOT-A", LocalDate.of(2026, 8, 20), null, ORDERED_PRICE))
                .thenReturn(31L);
        stubSave();

        Inbound result = inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ);

        assertThat(result.getStatus()).isEqualTo(InboundStatus.INSPECTING);
        InOrder order = inOrder(inboundRepository);
        order.verify(inboundRepository).save(any(Inbound.class));
        order.verify(inboundRepository).deleteLinesByInboundId(7L);
        ArgumentCaptor<List<InboundLine>> captor = ArgumentCaptor.forClass(List.class);
        order.verify(inboundRepository).saveLines(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        InboundLine saved = captor.getValue().get(0);
        assertThat(saved.getInboundId()).isEqualTo(7L);
        assertThat(saved.getPurchaseOrderLineId()).isEqualTo(11L);
        assertThat(saved.getLotId()).isEqualTo(31L);
        assertThat(saved.getReceivedQuantity()).isEqualTo(60L);
        assertThat(saved.getAcceptedQuantity()).isEqualTo(58L);
        assertThat(saved.getDefectiveQuantity()).isEqualTo(2L);
        assertThat(saved.getAcceptedSectionId()).isEqualTo(2L);
        assertThat(saved.getDefectSectionId()).isEqualTo(9L);
        assertThat(saved.getLineAmount()).isEqualByComparingTo("3600000");
        assertThat(saved.getReceivedBy()).isEqualTo(USER);
        assertThat(saved.getReceivedAt()).isNotNull();
        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.INBOUND, 7L, "ARRIVED", "INSPECTING", null, USER);
    }

    @Test
    @DisplayName("이미 검수 중인 입고를 다시 검수해도 상태가 바뀌지 않으므로 상태 이력을 남기지 않는다")
    void inspect_reInspect_noStatusHistory() {
        givenInbound(InboundStatus.INSPECTING);
        givenPurchaseOrderLine(100, 0);
        givenHappySections();
        when(lotPort.findOrRegisterLot(1L, 3L, "LOT-A", LocalDate.of(2026, 8, 20), null, ORDERED_PRICE))
                .thenReturn(31L);
        stubSave();

        inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ);

        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("검수 중 재호출은 상태를 유지한 채 항목만 다시 저장한다")
    void inspect_reinspect() {
        givenInbound(InboundStatus.INSPECTING);
        givenPurchaseOrderLine(100, 0);
        givenHappySections();
        when(lotPort.findOrRegisterLot(any(), any(), any(), any(), any(), any())).thenReturn(31L);
        stubSave();

        Inbound result = inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ);

        assertThat(result.getStatus()).isEqualTo(InboundStatus.INSPECTING);
        verify(inboundRepository).deleteLinesByInboundId(7L);
        verify(inboundRepository).saveLines(anyList());
    }

    @Test
    @DisplayName("구역은 나중에 지정할 수 있어 구역 없이도 검수 항목을 저장한다")
    void inspect_withoutSections() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 0);
        when(lotPort.findOrRegisterLot(any(), any(), any(), any(), any(), any())).thenReturn(31L);
        stubSave();

        inboundInspectService.inspectInbound(7L, command(withSections(line(60, 58, 2), null, null)), HQ);

        ArgumentCaptor<List<InboundLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(inboundRepository).saveLines(captor.capture());
        assertThat(captor.getValue().get(0).getAcceptedSectionId()).isNull();
        assertThat(captor.getValue().get(0).getDefectSectionId()).isNull();
        verifyNoInteractions(inboundSectionPort);
    }

    @Test
    @DisplayName("같은 발주 항목을 로트를 달리해 나눠 받을 수 있고, 잔여 수량과 같은 합계는 허용한다")
    void inspect_splitLots_withinRemaining() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 40);
        when(inboundSectionPort.getSection(2L)).thenReturn(rack(2L, 1L, true));
        when(lotPort.findOrRegisterLot(any(), any(), eq("LOT-A"), any(), any(), any())).thenReturn(31L);
        when(lotPort.findOrRegisterLot(any(), any(), eq("LOT-B"), any(), any(), any())).thenReturn(32L);
        stubSave();

        inboundInspectService.inspectInbound(7L, command(line("LOT-A", 40), line("LOT-B", 20)), HQ);

        ArgumentCaptor<List<InboundLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(inboundRepository).saveLines(captor.capture());
        assertThat(captor.getValue()).extracting(InboundLine::getLotId).containsExactly(31L, 32L);
    }

    @Test
    @DisplayName("발주 단가와 다른 입고 단가는 변경 사유가 있으면 저장한다")
    void inspect_priceChangedWithReason() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 0);
        givenHappySections();
        BigDecimal newPrice = BigDecimal.valueOf(58000.50);
        when(lotPort.findOrRegisterLot(any(), any(), any(), any(), any(), eq(newPrice))).thenReturn(31L);
        stubSave();

        inboundInspectService.inspectInbound(7L, command(withPrice(line(60, 58, 2), newPrice, "공급처 단가 인하")), HQ);

        ArgumentCaptor<List<InboundLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(inboundRepository).saveLines(captor.capture());
        assertThat(captor.getValue().get(0).getPriceChangeReason()).isEqualTo("공급처 단가 인하");
        assertThat(captor.getValue().get(0).getLineAmount()).isEqualByComparingTo("3480030");
    }

    // ---------- 요청 검증 ----------

    @Test
    @DisplayName("요청 자체가 잘못되면 DB를 조회하지 않고 VALIDATION_ERROR를 던진다")
    void inspect_invalidRequest() {
        InboundInspectCommand.Line base = line(60, 58, 2);
        List<InboundInspectCommand> invalid = List.of(
                new InboundInspectCommand(null, List.of(base)),
                new InboundInspectCommand(USER, null),
                new InboundInspectCommand(USER, List.of()),
                command(withLot(base, null, null, null)),
                command(withLot(base, " ", null, null)),
                command(withLot(base, "L".repeat(101), null, null)),
                command(withLot(base, "LOT-A", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1))),
                command(line(0, 0, 0)),
                command(line(60, -1, 61)),
                command(line(60, 61, -1)),
                command(line(60, 50, 5)),
                command(withPrice(base, null, null)),
                command(withPrice(base, BigDecimal.valueOf(-1), "사유")),
                command(withPrice(base, new BigDecimal("100.001"), "사유")),
                command(withNotes(base, "가".repeat(501), null)),
                command(withNotes(base, null, "가".repeat(1001))),
                command(line("LOT-A", 10), line("LOT-A", 10)));

        for (InboundInspectCommand command : invalid) {
            assertError(() -> inboundInspectService.inspectInbound(7L, command, HQ), ErrorCode.VALIDATION_ERROR.name());
        }
        verifyNoInteractions(inboundRepository, purchaseOrderRepository, lotPort, inboundSectionPort);
    }

    // ---------- 상태·참조 ----------

    @Test
    @DisplayName("없는 입고를 검수하면 INBOUND_NOT_FOUND를 던진다")
    void inspect_inboundNotFound() {
        when(inboundRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertError(() -> inboundInspectService.inspectInbound(999L, command(line(60, 58, 2)), HQ),
                InboundErrorCode.INBOUND_NOT_FOUND.name());
    }

    @Test
    @DisplayName("완료·취소된 입고는 검수할 수 없다")
    void inspect_notInspectable() {
        for (InboundStatus status : List.of(InboundStatus.COMPLETED, InboundStatus.CANCELED)) {
            when(inboundRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(inbound(status)));

            assertError(() -> inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ),
                    ErrorCode.CONFLICT.name());
        }
        verify(inboundRepository, never()).save(any(Inbound.class));
        verifyNoInteractions(lotPort);
    }

    @Test
    @DisplayName("입고의 발주를 찾을 수 없으면 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void inspect_purchaseOrderNotFound() {
        when(inboundRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(inbound(InboundStatus.ARRIVED)));
        when(purchaseOrderRepository.findById(4L)).thenReturn(Optional.empty());

        assertError(() -> inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
    }

    @Test
    @DisplayName("없는 발주 항목이면 NOT_FOUND를 던진다")
    void inspect_purchaseOrderLineNotFound() {
        givenInbound(InboundStatus.ARRIVED);
        when(purchaseOrderRepository.findLineById(11L)).thenReturn(Optional.empty());

        assertError(() -> inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ),
                ErrorCode.NOT_FOUND.name());
    }

    @Test
    @DisplayName("다른 발주의 발주 항목이면 VALIDATION_ERROR를 던진다")
    void inspect_purchaseOrderLineOfOtherOrder() {
        givenInbound(InboundStatus.ARRIVED);
        when(purchaseOrderRepository.findLineById(11L))
                .thenReturn(Optional.of(purchaseOrderLine(11L, 99L, 100, 0)));

        assertError(() -> inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(lotPort);
    }

    // ---------- 단가·수량 ----------

    @Test
    @DisplayName("발주 단가와 다른데 변경 사유가 없거나 공백이면 VALIDATION_ERROR를 던진다")
    void inspect_priceChangedWithoutReason() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 0);
        BigDecimal newPrice = BigDecimal.valueOf(55000);

        assertError(() -> inboundInspectService.inspectInbound(
                        7L, command(withPrice(line(60, 58, 2), newPrice, null)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> inboundInspectService.inspectInbound(
                        7L, command(withPrice(line(60, 58, 2), newPrice, "  ")), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(lotPort);
    }

    @Test
    @DisplayName("입고 수량이 발주 잔여 수량을 넘으면 VALIDATION_ERROR를 던진다")
    void inspect_exceedsRemaining() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 40);

        assertError(() -> inboundInspectService.inspectInbound(7L, command(line(61, 59, 2)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(lotPort);
    }

    @Test
    @DisplayName("로트를 나눠도 같은 발주 항목의 합계가 잔여 수량을 넘으면 VALIDATION_ERROR를 던진다")
    void inspect_splitLotsExceedRemaining() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 40);

        assertError(() -> inboundInspectService.inspectInbound(
                        7L, command(line("LOT-A", 40), line("LOT-B", 21)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(lotPort);
    }

    // ---------- 구역 ----------

    @Test
    @DisplayName("없는 구역이면 구역 도메인의 오류를 그대로 던진다")
    void inspect_sectionNotFound() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 0);
        when(inboundSectionPort.getSection(2L)).thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "구역 없음"));

        assertError(() -> inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ),
                ErrorCode.NOT_FOUND.name());
        verifyNoInteractions(lotPort);
    }

    @Test
    @DisplayName("다른 창고의 구역이면 VALIDATION_ERROR를 던진다")
    void inspect_sectionOfOtherWarehouse() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 0);
        when(inboundSectionPort.getSection(2L)).thenReturn(rack(2L, 99L, true));

        assertError(() -> inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("비활성 구역이면 VALIDATION_ERROR를 던진다")
    void inspect_inactiveSection() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 0);
        when(inboundSectionPort.getSection(2L)).thenReturn(rack(2L, 1L, false));

        assertError(() -> inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("합격품 구역이 불량 구역이면 VALIDATION_ERROR를 던진다")
    void inspect_acceptedInDefectSection() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 0);
        when(inboundSectionPort.getSection(2L)).thenReturn(defect(2L, 1L, true));

        assertError(() -> inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("불량품 구역이 불량 구역이 아니면 VALIDATION_ERROR를 던진다")
    void inspect_defectInNormalSection() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 0);
        when(inboundSectionPort.getSection(2L)).thenReturn(rack(2L, 1L, true));
        when(inboundSectionPort.getSection(9L)).thenReturn(rack(9L, 1L, true));

        assertError(() -> inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ),
                ErrorCode.VALIDATION_ERROR.name());
    }

    // ---------- 로트 ----------

    @Test
    @DisplayName("기존 로트와 원가·일자·상태가 맞지 않으면 재고 도메인의 409를 그대로 던지고 항목을 바꾸지 않는다")
    void inspect_lotConflictPropagates() {
        givenInbound(InboundStatus.ARRIVED);
        givenPurchaseOrderLine(100, 0);
        givenHappySections();
        for (InventoryErrorCode code : List.of(InventoryErrorCode.LOT_UNIT_COST_MISMATCH,
                InventoryErrorCode.LOT_DATE_MISMATCH, InventoryErrorCode.LOT_NOT_AVAILABLE)) {
            doThrow(new BusinessException(code)).when(lotPort)
                    .findOrRegisterLot(anyLong(), anyLong(), any(), any(), any(), any());

            assertError(() -> inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), HQ), code.name());
        }
        verify(inboundRepository, never()).save(any(Inbound.class));
        verify(inboundRepository, never()).deleteLinesByInboundId(anyLong());
        verify(inboundRepository, never()).saveLines(anyList());
    }


    @Test
    @DisplayName("검수는 입고 창고가 담당 창고가 아니면 403이고 상태·항목을 바꾸지 않는다")
    void inspect_otherWarehouse_forbidden() {
        AuthenticatedUser other = new AuthenticatedUser(6L, UserRole.WAREHOUSE_MANAGER, List.of(9L), List.of());
        when(inboundRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(inbound(InboundStatus.ARRIVED)));

        assertThatThrownBy(() -> inboundInspectService.inspectInbound(7L, command(line(60, 58, 2)), other))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(inboundRepository, never()).save(any());
    }
}

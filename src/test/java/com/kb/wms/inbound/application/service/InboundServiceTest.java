package com.kb.wms.inbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.inbound.application.port.in.command.InboundCancelCommand;
import com.kb.wms.inbound.application.port.in.command.InboundRegisterCommand;
import com.kb.wms.inbound.application.port.in.query.InboundSearchCondition;
import com.kb.wms.inbound.application.port.in.query.SectionCandidateCondition;
import com.kb.wms.inbound.application.port.in.result.InboundDetails;
import com.kb.wms.inbound.application.port.in.result.InboundLineView;
import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.application.port.in.result.SectionCandidate;
import com.kb.wms.inbound.application.port.out.InboundQueryRepository;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.exception.InboundErrorCode;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;

@ExtendWith(MockitoExtension.class)
class InboundServiceTest {

    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Mock
    private InboundRepository inboundRepository;

    @Mock
    private InboundQueryRepository inboundQueryRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private StatusHistoryUseCase statusHistoryUseCase;

    @InjectMocks
    private InboundService inboundService;

    // ---------- fixtures ----------

    private static PurchaseOrder purchaseOrder(PurchaseOrderStatus status) {
        return PurchaseOrder.builder()
                .purchaseOrderId(4L)
                .purchaseOrderNo("PO-20261002-0001")
                .warehouseId(1L)
                .supplierId(3L)
                .status(status)
                .createdBy(5L)
                .build();
    }

    private static Inbound inbound(Long id, InboundStatus status) {
        return Inbound.builder()
                .inboundId(id)
                .inboundNo("IB-20261002-0001")
                .purchaseOrderId(4L)
                .warehouseId(1L)
                .status(status)
                .arrivedAt(LocalDateTime.of(2026, 10, 2, 9, 0))
                .build();
    }

    private static InboundView viewWithStatus(InboundStatus status) {
        return new InboundView(
                7L, "IB-20261002-0001", 4L, "PO-20261002-0001", PurchaseOrderStatus.CONFIRMED, 3L, "공급처 A",
                1L, "서울 물류센터", status, LocalDateTime.of(2026, 10, 2, 9, 0),
                null, null, null, null, 0L, LocalDateTime.of(2026, 10, 2, 9, 5), LocalDateTime.of(2026, 10, 2, 10, 0));
    }

    private static void assertError(ThrowingCallable call, String errorCodeName) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(errorCodeName);
    }

    private void stubSaveAssigningId(Long id) {
        when(inboundRepository.save(any(Inbound.class))).thenAnswer(invocation -> {
            Inbound source = invocation.getArgument(0);
            return Inbound.builder()
                    .inboundId(id)
                    .inboundNo(source.getInboundNo())
                    .purchaseOrderId(source.getPurchaseOrderId())
                    .warehouseId(source.getWarehouseId())
                    .status(source.getStatus())
                    .arrivedAt(source.getArrivedAt())
                    .note(source.getNote())
                    .build();
        });
    }

    // ---------- 등록 ----------

    @Test
    @DisplayName("확정된 발주에 입고를 등록하면 발주의 창고로 ARRIVED 입고가 만들어지고 입고 ID를 반환한다")
    void register_success() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(PurchaseOrderStatus.CONFIRMED)));
        when(inboundRepository.existsInProgressByPurchaseOrderId(4L)).thenReturn(false);
        when(inboundRepository.countByInboundNoPrefix(any())).thenReturn(2L);
        stubSaveAssigningId(7L);
        LocalDateTime arrivedAt = LocalDateTime.now().minusHours(1);

        Long inboundId = inboundService.registerInbound(new InboundRegisterCommand(4L, arrivedAt, "1차 입고", 5L), HQ);

        assertThat(inboundId).isEqualTo(7L);
        ArgumentCaptor<Inbound> captor = ArgumentCaptor.forClass(Inbound.class);
        verify(inboundRepository).save(captor.capture());
        Inbound saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(InboundStatus.ARRIVED);
        assertThat(saved.getPurchaseOrderId()).isEqualTo(4L);
        assertThat(saved.getWarehouseId()).isEqualTo(1L);
        assertThat(saved.getArrivedAt()).isEqualTo(arrivedAt);
        assertThat(saved.getNote()).isEqualTo("1차 입고");
        assertThat(saved.getInboundNo())
                .isEqualTo("IB-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-0003");
        verify(statusHistoryUseCase).record(
                StatusHistoryEntityType.INBOUND, 7L, null, "ARRIVED", null, 5L);
    }

    @Test
    @DisplayName("도착 일시를 생략하면 요청 시각으로 채운다")
    void register_defaultArrivedAt() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(PurchaseOrderStatus.CONFIRMED)));
        when(inboundRepository.existsInProgressByPurchaseOrderId(4L)).thenReturn(false);
        when(inboundRepository.countByInboundNoPrefix(any())).thenReturn(0L);
        stubSaveAssigningId(7L);
        LocalDateTime before = LocalDateTime.now();

        inboundService.registerInbound(new InboundRegisterCommand(4L, null, null, 5L), HQ);

        ArgumentCaptor<Inbound> captor = ArgumentCaptor.forClass(Inbound.class);
        verify(inboundRepository).save(captor.capture());
        assertThat(captor.getValue().getArrivedAt()).isAfterOrEqualTo(before);
        assertThat(captor.getValue().getInboundNo()).endsWith("-0001");
    }

    @Test
    @DisplayName("발주 ID가 없으면 VALIDATION_ERROR를 던진다")
    void register_missingPurchaseOrderId() {
        assertError(() -> inboundService.registerInbound(new InboundRegisterCommand(null, null, null, 5L), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, inboundRepository);
    }

    @Test
    @DisplayName("처리 사용자가 없으면 VALIDATION_ERROR를 던진다")
    void register_missingUserId() {
        assertError(() -> inboundService.registerInbound(new InboundRegisterCommand(4L, null, null, null), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, inboundRepository, statusHistoryUseCase);
    }

    @Test
    @DisplayName("도착 일시가 미래이면 VALIDATION_ERROR를 던진다")
    void register_arrivedAtInFuture() {
        assertError(() -> inboundService.registerInbound(
                        new InboundRegisterCommand(4L, LocalDateTime.now().plusDays(1), null, 5L), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, inboundRepository);
    }

    @Test
    @DisplayName("비고가 1000자를 넘으면 VALIDATION_ERROR를 던진다")
    void register_noteTooLong() {
        assertError(() -> inboundService.registerInbound(
                        new InboundRegisterCommand(4L, null, "가".repeat(1001), 5L), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(purchaseOrderRepository, inboundRepository);
    }

    @Test
    @DisplayName("없는 발주에 입고를 등록하면 PURCHASE_ORDER_NOT_FOUND를 던진다")
    void register_purchaseOrderNotFound() {
        when(purchaseOrderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertError(() -> inboundService.registerInbound(new InboundRegisterCommand(999L, null, null, 5L), HQ),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
        verify(inboundRepository, never()).save(any(Inbound.class));
    }

    @Test
    @DisplayName("확정(CONFIRMED) 상태가 아닌 발주에는 입고를 등록할 수 없다")
    void register_purchaseOrderNotConfirmed() {
        for (PurchaseOrderStatus status : List.of(
                PurchaseOrderStatus.REQUESTED, PurchaseOrderStatus.COMPLETED, PurchaseOrderStatus.CANCELED)) {
            when(purchaseOrderRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(purchaseOrder(status)));

            assertError(() -> inboundService.registerInbound(new InboundRegisterCommand(4L, null, null, 5L), HQ),
                    ErrorCode.CONFLICT.name());
        }
        verify(inboundRepository, never()).save(any(Inbound.class));
    }

    @Test
    @DisplayName("같은 발주에 완료되지 않은 입고가 있으면 INBOUND_IN_PROGRESS를 던진다")
    void register_inboundInProgress() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(purchaseOrder(PurchaseOrderStatus.CONFIRMED)));
        when(inboundRepository.existsInProgressByPurchaseOrderId(4L)).thenReturn(true);

        assertError(() -> inboundService.registerInbound(new InboundRegisterCommand(4L, null, null, 5L), HQ),
                InboundErrorCode.INBOUND_IN_PROGRESS.name());
        verify(inboundRepository, never()).save(any(Inbound.class));
    }

    // ---------- 취소 ----------

    @Test
    @DisplayName("ARRIVED·INSPECTING 입고는 사유와 함께 취소할 수 있다")
    void cancel_success() {
        for (InboundStatus status : List.of(InboundStatus.ARRIVED, InboundStatus.INSPECTING)) {
            when(inboundRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(inbound(7L, status)));
            when(inboundRepository.save(any(Inbound.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Inbound result = inboundService.cancelInbound(7L, new InboundCancelCommand("잘못된 발주에 등록", 5L), HQ);

            assertThat(result.getStatus()).isEqualTo(InboundStatus.CANCELED);
            verify(statusHistoryUseCase).record(StatusHistoryEntityType.INBOUND, 7L,
                    status.name(), "CANCELED", "잘못된 발주에 등록", 5L);
        }
    }

    @Test
    @DisplayName("취소 처리 사용자가 없으면 VALIDATION_ERROR를 던지고 이력을 남기지 않는다")
    void cancel_userIdRequired() {
        assertError(() -> inboundService.cancelInbound(7L, new InboundCancelCommand("사유", null), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(inboundRepository, statusHistoryUseCase);
    }

    @Test
    @DisplayName("취소 사유가 없거나 공백이면 입고를 조회하지 않고 VALIDATION_ERROR를 던진다")
    void cancel_reasonRequired() {
        assertError(() -> inboundService.cancelInbound(7L, null, HQ), ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> inboundService.cancelInbound(7L, new InboundCancelCommand(null, 5L), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> inboundService.cancelInbound(7L, new InboundCancelCommand("  ", 5L), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(inboundRepository);
    }

    @Test
    @DisplayName("취소 사유가 500자를 넘으면 VALIDATION_ERROR를 던진다")
    void cancel_reasonTooLong() {
        assertError(() -> inboundService.cancelInbound(7L, new InboundCancelCommand("가".repeat(501), 5L), HQ),
                ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(inboundRepository);
    }

    @Test
    @DisplayName("없는 입고를 취소하면 INBOUND_NOT_FOUND를 던진다")
    void cancel_notFound() {
        when(inboundRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertError(() -> inboundService.cancelInbound(999L, new InboundCancelCommand("사유", 5L), HQ),
                InboundErrorCode.INBOUND_NOT_FOUND.name());
    }

    @Test
    @DisplayName("완료·취소된 입고는 취소할 수 없고 저장하지 않는다")
    void cancel_notCancelable() {
        for (InboundStatus status : List.of(InboundStatus.COMPLETED, InboundStatus.CANCELED)) {
            when(inboundRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(inbound(7L, status)));

            assertError(() -> inboundService.cancelInbound(7L, new InboundCancelCommand("사유", 5L), HQ),
                    ErrorCode.CONFLICT.name());
        }
        verify(inboundRepository, never()).save(any(Inbound.class));
        verifyNoInteractions(statusHistoryUseCase);
    }

    // ---------- 취소 사유 조회 ----------

    @Test
    @DisplayName("취소된 입고를 조회하면 상태 이력의 취소 사유를 cancelReason으로 채운다")
    void getInbound_canceled_fillsCancelReason() {
        when(inboundQueryRepository.findView(7L)).thenReturn(Optional.of(viewWithStatus(InboundStatus.CANCELED)));
        when(statusHistoryUseCase.findStatusReason(StatusHistoryEntityType.INBOUND, 7L, "CANCELED"))
                .thenReturn(Optional.of("잘못된 발주에 등록"));

        assertThat(inboundService.getInbound(7L, HQ).cancelReason()).isEqualTo("잘못된 발주에 등록");
    }

    @Test
    @DisplayName("취소 상태가 아닌 입고는 상태 이력을 조회하지 않고 cancelReason이 null이다")
    void getInbound_notCanceled_noCancelReason() {
        when(inboundQueryRepository.findView(7L)).thenReturn(Optional.of(viewWithStatus(InboundStatus.ARRIVED)));

        assertThat(inboundService.getInbound(7L, HQ).cancelReason()).isNull();
        verifyNoInteractions(statusHistoryUseCase);
    }

    // ---------- 조회 ----------

    @Test
    @DisplayName("입고 목록은 조건을 조회 리포지토리에 넘겨 결과를 그대로 반환한다")
    void getInbounds_delegates() {
        InboundSearchCondition condition = new InboundSearchCondition(
                InboundStatus.ARRIVED, 1L, 4L, "IB-", null, null);
        when(inboundQueryRepository.search(condition)).thenReturn(List.of());

        assertThat(inboundService.getInbounds(condition, HQ)).isEmpty();
        verify(inboundQueryRepository).search(condition);
    }

    @Test
    @DisplayName("도착 시작 일시가 종료 일시보다 늦으면 VALIDATION_ERROR를 던진다")
    void getInbounds_invalidDateRange() {
        InboundSearchCondition condition = new InboundSearchCondition(
                null, null, null, null,
                LocalDateTime.of(2026, 10, 2, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0));

        assertError(() -> inboundService.getInbounds(condition, HQ), ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(inboundQueryRepository);
    }

    @Test
    @DisplayName("없는 입고를 조회하면 INBOUND_NOT_FOUND를 던진다")
    void getInbound_notFound() {
        when(inboundQueryRepository.findView(999L)).thenReturn(Optional.empty());

        assertError(() -> inboundService.getInbound(999L, HQ), InboundErrorCode.INBOUND_NOT_FOUND.name());
    }

    @Test
    @DisplayName("입고 상세는 헤더 식별 정보와 검수 항목을 합쳐 반환한다")
    void getInboundDetails_success() {
        InboundView view = new InboundView(
                7L, "IB-20261002-0001", 4L, "PO-20261002-0001", PurchaseOrderStatus.CONFIRMED, 3L, "공급처 A",
                1L, "서울 물류센터", InboundStatus.INSPECTING, LocalDateTime.of(2026, 10, 2, 9, 0),
                null, null, null, null, 1L, LocalDateTime.of(2026, 10, 2, 9, 5), LocalDateTime.of(2026, 10, 2, 10, 0));
        InboundLineView lineView = new InboundLineView(
                21L, 11L, 1L, "SKU-0001", "배드민턴 라켓", 1L, "LOT-A", null, null,
                60L, 58L, 2L, 2L, "A-01", 9L, "D-01",
                BigDecimal.valueOf(60000), BigDecimal.valueOf(60000), BigDecimal.valueOf(3600000),
                null, null, LocalDateTime.of(2026, 10, 2, 10, 0), 5L);
        when(inboundQueryRepository.findView(7L)).thenReturn(Optional.of(view));
        when(inboundQueryRepository.findLineViews(7L)).thenReturn(List.of(lineView));

        InboundDetails details = inboundService.getInboundDetails(7L, HQ);

        assertThat(details.inboundId()).isEqualTo(7L);
        assertThat(details.inboundNo()).isEqualTo("IB-20261002-0001");
        assertThat(details.status()).isEqualTo(InboundStatus.INSPECTING);
        assertThat(details.items()).containsExactly(lineView);
    }

    @Test
    @DisplayName("없는 입고의 상세를 조회하면 항목을 조회하지 않고 INBOUND_NOT_FOUND를 던진다")
    void getInboundDetails_notFound() {
        when(inboundQueryRepository.findView(anyLong())).thenReturn(Optional.empty());

        assertError(() -> inboundService.getInboundDetails(999L, HQ), InboundErrorCode.INBOUND_NOT_FOUND.name());
        verify(inboundQueryRepository, never()).findLineViews(anyLong());
    }

    // ---------- 구역 후보 ----------

    @Test
    @DisplayName("합격 구역 후보는 입고 창고 기준으로 조회한다")
    void getAssignableSections_usesInboundWarehouse() {
        SectionCandidateCondition condition = new SectionCandidateCondition(BigDecimal.TEN, "A");
        SectionCandidate candidate = new SectionCandidate(
                2L, 1L, "A-01-R01", "A구역 1번 랙", "RACK",
                BigDecimal.valueOf(800), BigDecimal.valueOf(120), BigDecimal.valueOf(680));
        when(inboundRepository.findById(7L)).thenReturn(Optional.of(inbound(7L, InboundStatus.INSPECTING)));
        when(inboundQueryRepository.findAssignableSections(1L, condition)).thenReturn(List.of(candidate));

        assertThat(inboundService.getAssignableSections(7L, condition, HQ)).containsExactly(candidate);
    }

    @Test
    @DisplayName("불량 구역 후보는 입고 창고 기준으로 조회한다")
    void getDefectSections_usesInboundWarehouse() {
        SectionCandidateCondition condition = new SectionCandidateCondition(null, null);
        when(inboundRepository.findById(7L)).thenReturn(Optional.of(inbound(7L, InboundStatus.ARRIVED)));
        when(inboundQueryRepository.findDefectSections(1L, condition)).thenReturn(List.of());

        assertThat(inboundService.getDefectSections(7L, condition, HQ)).isEmpty();
    }

    @Test
    @DisplayName("없는 입고의 구역 후보를 조회하면 INBOUND_NOT_FOUND를 던진다")
    void getSections_inboundNotFound() {
        when(inboundRepository.findById(999L)).thenReturn(Optional.empty());
        SectionCandidateCondition condition = new SectionCandidateCondition(null, null);

        assertError(() -> inboundService.getAssignableSections(999L, condition, HQ),
                InboundErrorCode.INBOUND_NOT_FOUND.name());
        assertError(() -> inboundService.getDefectSections(999L, condition, HQ),
                InboundErrorCode.INBOUND_NOT_FOUND.name());
    }

    @Test
    @DisplayName("필요 수량이 음수이면 입고를 조회하지 않고 VALIDATION_ERROR를 던진다")
    void getSections_negativeRequiredQuantity() {
        SectionCandidateCondition condition = new SectionCandidateCondition(BigDecimal.valueOf(-1), null);

        assertError(() -> inboundService.getAssignableSections(7L, condition, HQ), ErrorCode.VALIDATION_ERROR.name());
        assertError(() -> inboundService.getDefectSections(7L, condition, HQ), ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(inboundRepository, inboundQueryRepository);
    }


    private static final AuthenticatedUser MANAGER =
            new AuthenticatedUser(5L, UserRole.WAREHOUSE_MANAGER, List.of(1L, 2L), List.of());
    private static final AuthenticatedUser OTHER_MANAGER =
            new AuthenticatedUser(6L, UserRole.WAREHOUSE_MANAGER, List.of(9L), List.of());

    @Test
    @DisplayName("입고 등록은 발주의 창고가 담당 창고일 때만 할 수 있다")
    void register_otherWarehouse_forbidden() {
        when(purchaseOrderRepository.findByIdForUpdate(4L))
                .thenReturn(java.util.Optional.of(purchaseOrder(PurchaseOrderStatus.CONFIRMED)));

        assertError(() -> inboundService.registerInbound(
                        new InboundRegisterCommand(4L, null, null, 6L), OTHER_MANAGER),
                ErrorCode.FORBIDDEN.name());
        verify(inboundRepository, never()).save(any());
    }

    @Test
    @DisplayName("입고 목록은 창고를 생략하면 담당 창고로 좁히고, 비담당 창고를 지정하면 403이다")
    void getInbounds_scope() {
        when(inboundQueryRepository.search(
                new InboundSearchCondition(null, null, null, null, null, null, List.of(1L, 2L))))
                .thenReturn(List.of());

        assertThat(inboundService.getInbounds(
                new InboundSearchCondition(null, null, null, null, null, null), MANAGER)).isEmpty();
        assertError(() -> inboundService.getInbounds(
                new InboundSearchCondition(null, 3L, null, null, null, null), MANAGER), ErrorCode.FORBIDDEN.name());
    }

    @Test
    @DisplayName("입고 단건·상세·구역 후보·취소는 입고 창고가 담당 창고가 아니면 403이다")
    void singleInbound_otherWarehouse_forbidden() {
        when(inboundQueryRepository.findView(7L)).thenReturn(java.util.Optional.of(viewWithStatus(InboundStatus.ARRIVED)));
        when(inboundRepository.findById(7L)).thenReturn(java.util.Optional.of(inbound(7L, InboundStatus.ARRIVED)));
        when(inboundRepository.findByIdForUpdate(7L)).thenReturn(java.util.Optional.of(inbound(7L, InboundStatus.ARRIVED)));
        SectionCandidateCondition condition = new SectionCandidateCondition(null, null);

        assertError(() -> inboundService.getInbound(7L, OTHER_MANAGER), ErrorCode.FORBIDDEN.name());
        assertError(() -> inboundService.getInboundDetails(7L, OTHER_MANAGER), ErrorCode.FORBIDDEN.name());
        assertError(() -> inboundService.getAssignableSections(7L, condition, OTHER_MANAGER), ErrorCode.FORBIDDEN.name());
        assertError(() -> inboundService.getDefectSections(7L, condition, OTHER_MANAGER), ErrorCode.FORBIDDEN.name());
        assertError(() -> inboundService.cancelInbound(7L, new InboundCancelCommand("사유", 6L), OTHER_MANAGER),
                ErrorCode.FORBIDDEN.name());
        verify(inboundRepository, never()).save(any());
        assertThat(inboundService.getInbound(7L, MANAGER).inboundId()).isEqualTo(7L);
    }
}

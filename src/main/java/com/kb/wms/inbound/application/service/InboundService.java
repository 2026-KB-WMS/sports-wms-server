package com.kb.wms.inbound.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.inbound.application.port.in.InboundUseCase;
import com.kb.wms.inbound.application.port.in.command.InboundCancelCommand;
import com.kb.wms.inbound.application.port.in.command.InboundRegisterCommand;
import com.kb.wms.inbound.application.port.in.query.InboundSearchCondition;
import com.kb.wms.inbound.application.port.in.query.SectionCandidateCondition;
import com.kb.wms.inbound.application.port.in.result.InboundDetails;
import com.kb.wms.inbound.application.port.in.result.InboundSummary;
import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.application.port.in.result.SectionCandidate;
import com.kb.wms.inbound.application.port.out.InboundQueryRepository;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.exception.InboundErrorCode;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 입고 등록·조회·취소와 검수 구역 후보 조회.
 *
 * <p>역할은 SecurityConfig가, 담당 창고 범위는 이 서비스가 검사한다(ADR-012).
 * 검수(inspect)와 완료(complete)는 각각 별도 서비스로 구현한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboundService implements InboundUseCase {

    private static final String NO_PREFIX = "IB-";
    private static final DateTimeFormatter NO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int NOTE_MAX_LENGTH = 1000;
    private static final int CANCEL_REASON_MAX_LENGTH = 500;

    private final InboundRepository inboundRepository;
    private final InboundQueryRepository inboundQueryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StatusHistoryUseCase statusHistoryUseCase;

    /**
     * 확정된 발주에 대해 ARRIVED 입고를 등록한다. 입고 대상 창고는 발주의 창고를 따른다.
     * 발주 행을 락으로 잡아 같은 발주의 동시 입고 등록·발주 취소와 어긋나지 않게 한다.
     */
    @Override
    @Transactional
    public Long registerInbound(InboundRegisterCommand command, AuthenticatedUser actor) {
        if (command.purchaseOrderId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "발주 ID는 필수입니다.");
        }
        if (command.userId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "처리 사용자는 필수입니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (command.arrivedAt() != null && command.arrivedAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "도착 일시는 현재 시각 이후일 수 없습니다.");
        }
        if (command.note() != null && command.note().length() > NOTE_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "비고는 " + NOTE_MAX_LENGTH + "자 이하여야 합니다.");
        }

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdForUpdate(command.purchaseOrderId())
                .orElseThrow(() -> new BusinessException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND));
        actor.requireWarehouseAccess(purchaseOrder.getWarehouseId());
        if (!purchaseOrder.isConfirmed()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "확정된 발주만 입고를 등록할 수 있습니다. 현재 상태: " + purchaseOrder.getStatus());
        }
        if (inboundRepository.existsInProgressByPurchaseOrderId(purchaseOrder.getPurchaseOrderId())) {
            throw new BusinessException(InboundErrorCode.INBOUND_IN_PROGRESS);
        }

        Inbound saved = inboundRepository.save(Inbound.register(
                nextInboundNo(LocalDate.now()), purchaseOrder.getPurchaseOrderId(), purchaseOrder.getWarehouseId(),
                command.arrivedAt() == null ? now : command.arrivedAt(), command.note()));
        statusHistoryUseCase.record(StatusHistoryEntityType.INBOUND, saved.getInboundId(),
                null, InboundStatus.ARRIVED.name(), null, command.userId());
        return saved.getInboundId();
    }

    @Override
    public List<InboundSummary> getInbounds(InboundSearchCondition condition, AuthenticatedUser actor) {
        List<Long> scope = actor.warehouseScope(condition.warehouseId());
        if (condition.arrivedFrom() != null && condition.arrivedTo() != null
                && condition.arrivedFrom().isAfter(condition.arrivedTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "도착 시작 일시는 종료 일시보다 늦을 수 없습니다.");
        }
        return inboundQueryRepository.search(new InboundSearchCondition(
                condition.status(), condition.warehouseId(), condition.purchaseOrderId(), condition.keyword(),
                condition.arrivedFrom(), condition.arrivedTo(), scope));
    }

    @Override
    public InboundView getInbound(Long inboundId, AuthenticatedUser actor) {
        InboundView view = inboundQueryRepository.findView(inboundId).orElseThrow(InboundService::notFound);
        actor.requireWarehouseAccess(view.warehouseId());
        if (view.status() != InboundStatus.CANCELED) {
            return view;
        }
        // 취소 사유는 CANCELED로 바뀔 때의 상태 이력에서 읽는다.
        return view.withCancelReason(statusHistoryUseCase
                .findStatusReason(StatusHistoryEntityType.INBOUND, inboundId, InboundStatus.CANCELED.name())
                .orElse(null));
    }

    @Override
    public InboundDetails getInboundDetails(Long inboundId, AuthenticatedUser actor) {
        InboundView view = getInbound(inboundId, actor);
        return new InboundDetails(
                view.inboundId(), view.inboundNo(), view.status(),
                inboundQueryRepository.findLineViews(inboundId));
    }

    /**
     * 완료 전(ARRIVED·INSPECTING) 입고를 취소한다. 검수 항목은 재고에 반영되기 전이라 그대로 두고,
     * 취소 사유와 처리자는 CANCELED 전이 이력(StatusHistory)에 남긴다. 응답의 cancelReason은 이 이력에서 읽는다.
     */
    @Override
    @Transactional
    public Inbound cancelInbound(Long inboundId, InboundCancelCommand command, AuthenticatedUser actor) {
        String reason = command == null ? null : command.reason();
        if (command == null || command.userId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "처리 사용자는 필수입니다.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "입고를 취소할 때는 사유를 입력해야 합니다.");
        }
        if (reason.length() > CANCEL_REASON_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "취소 사유는 " + CANCEL_REASON_MAX_LENGTH + "자 이하여야 합니다.");
        }

        Inbound inbound = inboundRepository.findByIdForUpdate(inboundId).orElseThrow(InboundService::notFound);
        actor.requireWarehouseAccess(inbound.getWarehouseId());
        if (!inbound.isCancelable()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "도착 또는 검수 중 상태의 입고만 취소할 수 있습니다. 현재 상태: " + inbound.getStatus());
        }
        InboundStatus from = inbound.getStatus();
        inbound.cancel();
        Inbound saved = inboundRepository.save(inbound);
        statusHistoryUseCase.record(StatusHistoryEntityType.INBOUND, inboundId,
                from.name(), saved.getStatus().name(), reason, command.userId());
        return saved;
    }

    @Override
    public List<SectionCandidate> getAssignableSections(Long inboundId, SectionCandidateCondition condition,
                                                     AuthenticatedUser actor) {
        validateSectionCondition(condition);
        Inbound inbound = findOrThrow(inboundId);
        actor.requireWarehouseAccess(inbound.getWarehouseId());
        return inboundQueryRepository.findAssignableSections(inbound.getWarehouseId(), condition);
    }

    @Override
    public List<SectionCandidate> getDefectSections(Long inboundId, SectionCandidateCondition condition,
                                                 AuthenticatedUser actor) {
        validateSectionCondition(condition);
        Inbound inbound = findOrThrow(inboundId);
        actor.requireWarehouseAccess(inbound.getWarehouseId());
        return inboundQueryRepository.findDefectSections(inbound.getWarehouseId(), condition);
    }

    /**
     * IB-YYYYMMDD-일련번호(4자리). 당일 입고 개수 + 1로 채번하며, 동시 등록으로 번호가 겹치면
     * UNIQUE 제약이 409 DUPLICATE_INBOUND_NO로 응답한다.
     */
    private String nextInboundNo(LocalDate today) {
        String prefix = NO_PREFIX + today.format(NO_DATE_FORMAT) + "-";
        long sequence = inboundRepository.countByInboundNoPrefix(prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    private static void validateSectionCondition(SectionCandidateCondition condition) {
        if (condition.requiredQuantity() != null && condition.requiredQuantity().signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "필요 수량은 0 이상이어야 합니다.");
        }
    }

    private Inbound findOrThrow(Long inboundId) {
        return inboundRepository.findById(inboundId).orElseThrow(InboundService::notFound);
    }

    private static BusinessException notFound() {
        return new BusinessException(InboundErrorCode.INBOUND_NOT_FOUND);
    }
}

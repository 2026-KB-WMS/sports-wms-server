package com.kb.wms.outbound.application.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.outbound.application.port.in.OutboundUseCase;
import com.kb.wms.outbound.application.port.in.command.OutboundCancelCommand;
import com.kb.wms.outbound.application.port.in.command.OutboundCreateCommand;
import com.kb.wms.outbound.application.port.in.query.OutboundSearchCondition;
import com.kb.wms.outbound.application.port.in.result.OutboundCancelResult;
import com.kb.wms.outbound.application.port.in.result.OutboundCreateResult;
import com.kb.wms.outbound.application.port.in.result.OutboundDetail;
import com.kb.wms.outbound.application.port.in.result.OutboundLineView;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingStartResult;
import com.kb.wms.outbound.application.port.in.result.OutboundSummary;
import com.kb.wms.outbound.application.port.in.result.OutboundView;
import com.kb.wms.outbound.application.port.out.OutboundQueryRepository;
import com.kb.wms.outbound.application.port.out.OutboundRepository;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.entity.OutboundLine;
import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.outbound.exception.OutboundErrorCode;
import com.kb.wms.storeorder.application.port.in.StoreOrderFulfillmentUseCase;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

import lombok.RequiredArgsConstructor;

/**
 * 출고 생성·피킹 시작·취소와 조회(서비스 B). 상태 확인과 변경은 발주 헤더 → 출고 헤더 순으로 잠그고 처리한다.
 * 발주 취소(승인 이후)가 피킹 시작 여부를 확인할 때 같은 발주 잠금을 쓰므로, 피킹 시작과 발주 취소가 동시에 들어와도
 * 한쪽만 성공한다. 전역 락 순서는 성능 개선 단계에서 다룬다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboundService implements OutboundUseCase {

    static final int NOTE_MAX_LENGTH = 500;
    static final int REASON_MAX_LENGTH = 500;

    private static final String NO_PREFIX = "OB-";
    private static final DateTimeFormatter NO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final OutboundRepository outboundRepository;
    private final StockAllocationRepository stockAllocationRepository;
    private final OutboundQueryRepository outboundQueryRepository;
    private final StoreOrderFulfillmentUseCase storeOrderFulfillmentUseCase;
    private final StatusHistoryUseCase statusHistoryUseCase;

    @Override
    @Transactional
    public OutboundCreateResult createOutbound(OutboundCreateCommand command) {
        requireUser(command.userId());
        if (command.storeOrderId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "출고할 발주를 선택해주세요.");
        }
        String note = normalize(command.note());
        if (note != null && note.length() > NOTE_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "비고는 " + NOTE_MAX_LENGTH + "자 이하여야 합니다.");
        }

        StoreOrder order = storeOrderFulfillmentUseCase.getOrderForUpdate(command.storeOrderId());
        if (order.getStatus() != StoreOrderStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "배정된 발주만 출고를 만들 수 있습니다. 현재 상태: " + order.getStatus());
        }

        List<StockAllocation> allocations = stockAllocationRepository
                .findUnlinkedAllocatedByStoreOrderIdForUpdate(order.getStoreOrderId());
        if (allocations.isEmpty()) {
            throw new BusinessException(OutboundErrorCode.NO_ALLOCATION);
        }

        Outbound outbound = outboundRepository.save(
                Outbound.create(nextOutboundNo(LocalDate.now()), order.getStoreOrderId(), note));
        outboundRepository.saveLines(allocations.stream()
                .map(a -> OutboundLine.create(outbound.getOutboundId(), a.getAllocationId()))
                .toList());
        statusHistoryUseCase.record(StatusHistoryEntityType.OUTBOUND, outbound.getOutboundId(),
                null, OutboundStatus.READY.name(), null, command.userId());

        OutboundView view = findViewOrThrow(outbound.getOutboundId());
        List<OutboundLineView> items = outboundQueryRepository.findOutboundLineViews(outbound.getOutboundId());
        return new OutboundCreateResult(view, items);
    }

    @Override
    @Transactional
    public OutboundPickingStartResult startPicking(Long outboundId, Long userId) {
        requireUser(userId);
        Outbound outbound = lockOutbound(outboundId);

        if (outbound.getStatus() != OutboundStatus.READY) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "READY 상태의 출고만 피킹을 시작할 수 있습니다. 현재 상태: " + outbound.getStatus());
        }
        StoreOrder order = storeOrderFulfillmentUseCase.getOrder(outbound.getStoreOrderId());
        if (order.getStatus() != StoreOrderStatus.ASSIGNED) {
            throw new BusinessException(OutboundErrorCode.ORDER_NOT_ASSIGNED,
                    OutboundErrorCode.ORDER_NOT_ASSIGNED.getDefaultMessage() + " 현재 상태: " + order.getStatus());
        }

        List<Long> allocationIds = outboundRepository.findLinesByOutboundId(outboundId).stream()
                .map(OutboundLine::getAllocationId)
                .sorted()
                .toList();
        List<StockAllocation> allocations = stockAllocationRepository.findAllByIdForUpdate(allocationIds);
        if (allocations.size() != allocationIds.size()
                || allocations.stream().anyMatch(a -> a.getStatus() != AllocationStatus.ALLOCATED)) {
            throw new BusinessException(OutboundErrorCode.ALLOCATION_NOT_ACTIVE);
        }

        outbound.startPicking();
        outboundRepository.save(outbound);
        statusHistoryUseCase.record(StatusHistoryEntityType.OUTBOUND, outboundId,
                OutboundStatus.READY.name(), OutboundStatus.PICKING.name(), null, userId);

        OutboundView view = findViewOrThrow(outboundId);
        return new OutboundPickingStartResult(view.outboundId(), view.outboundNo(), view.status(), view.updatedAt());
    }

    @Override
    @Transactional
    public OutboundCancelResult cancel(OutboundCancelCommand command) {
        requireUser(command.userId());
        String reason = normalize(command.reason());
        if (reason == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "출고 취소 사유를 입력해주세요.");
        }
        if (reason.length() > REASON_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "취소 사유는 " + REASON_MAX_LENGTH + "자 이하여야 합니다.");
        }
        Outbound outbound = lockOutbound(command.outboundId());

        if (outbound.getStatus() != OutboundStatus.READY) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "READY 상태의 출고만 취소할 수 있습니다. 현재 상태: " + outbound.getStatus());
        }

        outbound.cancel();
        outboundRepository.save(outbound);
        statusHistoryUseCase.record(StatusHistoryEntityType.OUTBOUND, command.outboundId(),
                OutboundStatus.READY.name(), OutboundStatus.CANCELED.name(), reason, command.userId());

        OutboundView view = findViewOrThrow(command.outboundId());
        return new OutboundCancelResult(view.outboundId(), view.outboundNo(), view.status(), reason,
                view.updatedAt());
    }

    @Override
    public List<OutboundSummary> searchOutbounds(OutboundSearchCondition condition) {
        if (condition.createdFrom() != null && condition.createdTo() != null
                && condition.createdFrom().isAfter(condition.createdTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "조회 시작 일시가 종료 일시보다 늦을 수 없습니다.");
        }
        return outboundQueryRepository.searchOutbounds(condition);
    }

    @Override
    public OutboundDetail getOutbound(Long outboundId) {
        OutboundView view = findViewOrThrow(outboundId);
        List<OutboundLineView> items = outboundQueryRepository.findOutboundLineViews(outboundId);
        String cancelReason = view.status() == OutboundStatus.CANCELED
                ? statusHistoryUseCase
                        .findStatusReason(StatusHistoryEntityType.OUTBOUND, outboundId, OutboundStatus.CANCELED.name())
                        .orElse(null)
                : null;
        return new OutboundDetail(view, items, cancelReason);
    }

    /**
     * 발주 → 출고 순으로 잠그기 위해, 잠그지 않는 조회로 발주를 먼저 찾아 잠근 뒤 출고를 잠근다.
     * 발주 취소(승인 이후)가 발주를 잠근 채 출고를 처리하는 순서와 같다.
     */
    private Outbound lockOutbound(Long outboundId) {
        if (outboundId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "출고를 선택해주세요.");
        }
        OutboundView view = findViewOrThrow(outboundId);
        storeOrderFulfillmentUseCase.getOrderForUpdate(view.storeOrderId());
        return outboundRepository.findByIdForUpdate(outboundId)
                .orElseThrow(() -> new BusinessException(OutboundErrorCode.OUTBOUND_NOT_FOUND));
    }

    private OutboundView findViewOrThrow(Long outboundId) {
        return outboundQueryRepository.findOutboundView(outboundId)
                .orElseThrow(() -> new BusinessException(OutboundErrorCode.OUTBOUND_NOT_FOUND));
    }

    /**
     * OB-YYYYMMDD-일련번호(4자리). 당일 출고 개수 + 1로 채번하며, 동시 생성으로 번호가 겹치면
     * UNIQUE 제약이 409 DUPLICATE_OUTBOUND_NO로 응답한다.
     */
    private String nextOutboundNo(LocalDate today) {
        String prefix = NO_PREFIX + today.format(NO_DATE_FORMAT) + "-";
        long sequence = outboundRepository.countByOutboundNoPrefix(prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청 사용자는 필수입니다.");
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

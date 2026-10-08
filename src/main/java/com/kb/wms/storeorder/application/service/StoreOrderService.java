package com.kb.wms.storeorder.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.storeorder.application.port.in.StoreOrderUseCase;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCancelCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderCompletePartialCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderHoldCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRegisterCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRejectCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderResumeCommand;
import com.kb.wms.storeorder.application.port.in.query.StoreOrderSearchCondition;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderAssignResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderCompletePartialResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetail;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetails;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderFulfillmentCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderListItem;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderStatusChange;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderStatusHistoryView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderSummary;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderView;
import com.kb.wms.storeorder.application.port.out.SkuAvailabilityPort;
import com.kb.wms.storeorder.application.port.out.SkuSupplyPricePort;
import com.kb.wms.storeorder.application.port.out.StoreAvailabilityPort;
import com.kb.wms.storeorder.application.port.out.StoreOrderOutboundPort;
import com.kb.wms.storeorder.application.port.out.StoreOrderQueryRepository;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.application.port.out.WarehouseAvailabilityPort;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;
import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderProgressStage;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.storeorder.exception.StoreOrderErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 지점 발주 등록·조회.
 *
 * <p>역할·소속 지점/창고·작성자 검사(등록은 배정된 지점의 점주, 목록은 본사 관리자, 단건·상세는 데이터 범위)와
 * {@code GET /orders/my}의 소속 판별은 인증 도메인 연동 시 웹 어댑터에서 적용한다.
 * 이 서비스는 입력·상태·참조 대상 규칙만 검증한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreOrderService implements StoreOrderUseCase {

    private static final String NO_PREFIX = "SO-";
    private static final DateTimeFormatter NO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int NOTE_MAX_LENGTH = 1000;
    private static final int REASON_MAX_LENGTH = 500;

    /** 응답의 statusReason을 내려주는 상태. 재개 후 ASSIGNED 등 사유가 필요 없는 상태에서는 null이다. */
    private static final Set<StoreOrderStatus> REASON_STATUSES =
            Set.of(StoreOrderStatus.REJECTED, StoreOrderStatus.CANCELED, StoreOrderStatus.ON_HOLD);

    private final StoreOrderRepository storeOrderRepository;
    private final StoreOrderQueryRepository storeOrderQueryRepository;
    private final StoreOrderOutboundPort storeOrderOutboundPort;
    private final StoreAvailabilityPort storeAvailabilityPort;
    private final WarehouseAvailabilityPort warehouseAvailabilityPort;
    private final SkuSupplyPricePort skuSupplyPricePort;
    private final SkuAvailabilityPort skuAvailabilityPort;
    private final StatusHistoryUseCase statusHistoryUseCase;

    @Override
    @Transactional
    public Long registerStoreOrder(StoreOrderRegisterCommand command, AuthenticatedUser actor) {
        validateRegister(command);
        actor.requireStoreAccess(command.storeId());

        storeAvailabilityPort.requireActive(command.storeId());

        // 공급 단가는 요청에서 받지 않고 등록 시점의 SKU 공급 단가를 스냅샷한다.
        List<BigDecimal> unitPrices = command.lines().stream()
                .map(line -> skuSupplyPricePort.getOrderableSupplyPrice(line.skuId()))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        StoreOrder saved = storeOrderRepository.save(StoreOrder.register(
                nextOrderNo(now.toLocalDate()), command.storeId(), now,
                command.requestedDeliveryAt(), command.note(), command.createdBy()));

        List<StoreOrderLine> lines = new ArrayList<>(command.lines().size());
        for (int i = 0; i < command.lines().size(); i++) {
            StoreOrderRegisterCommand.Line line = command.lines().get(i);
            lines.add(StoreOrderLine.register(
                    saved.getStoreOrderId(), line.skuId(), line.requestedQuantity(), unitPrices.get(i)));
        }
        storeOrderRepository.saveLines(lines);

        statusHistoryUseCase.record(StatusHistoryEntityType.STORE_ORDER, saved.getStoreOrderId(),
                null, StoreOrderStatus.REQUESTED.name(), null, command.createdBy());

        return saved.getStoreOrderId();
    }

    @Override
    public List<StoreOrderListItem> getStoreOrders(StoreOrderSearchCondition condition) {
        return searchStoreOrders(condition);
    }

    /**
     * 점주는 담당 지점의 발주, 창고 관리자는 담당 창고에 배정된 발주(창고 배정 전 발주는 제외)만 돌려준다.
     * 지점·창고를 필터로 지정하면 담당 범위여야 하고(아니면 403), 본사 관리자는 이 조회를 쓰지 않는다(403).
     */
    @Override
    public List<StoreOrderListItem> getMyStoreOrders(StoreOrderSearchCondition condition, AuthenticatedUser actor) {
        List<Long> storeScope = null;
        List<Long> warehouseScope = null;
        if (actor.isStoreOwner()) {
            if (condition.storeId() != null) {
                actor.requireStoreAccess(condition.storeId());
            } else {
                storeScope = actor.storeIds();
            }
        } else if (actor.isWarehouseManager()) {
            if (condition.warehouseId() != null) {
                actor.requireWarehouseAccess(condition.warehouseId());
            } else {
                warehouseScope = actor.warehouseIds();
            }
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return searchStoreOrders(new StoreOrderSearchCondition(
                condition.status(), condition.storeId(), condition.warehouseId(), condition.keyword(),
                condition.requestedFrom(), condition.requestedTo(), storeScope, warehouseScope));
    }

    private List<StoreOrderListItem> searchStoreOrders(StoreOrderSearchCondition condition) {
        if (condition.requestedFrom() != null && condition.requestedTo() != null
                && condition.requestedFrom().isAfter(condition.requestedTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청 시작 일시는 종료 일시보다 늦을 수 없습니다.");
        }
        if (condition.storeId() != null) {
            storeAvailabilityPort.requireExists(condition.storeId());
        }
        if (condition.warehouseId() != null) {
            warehouseAvailabilityPort.requireExists(condition.warehouseId());
        }

        List<StoreOrderSummary> summaries = storeOrderQueryRepository.search(condition);
        List<Long> orderIds = summaries.stream().map(StoreOrderSummary::storeOrderId).toList();
        Map<Long, StoreOrderOutboundStatus> latestStatuses =
                storeOrderOutboundPort.findLatestOutboundStatuses(orderIds);

        return summaries.stream()
                .map(summary -> {
                    StoreOrderOutboundStatus latest = latestStatuses.get(summary.storeOrderId());
                    return new StoreOrderListItem(summary, latest,
                            StoreOrderProgressStage.resolve(summary.status(), latest, summary.hasShortage()));
                })
                .toList();
    }

    @Override
    public StoreOrderDetail getStoreOrder(Long storeOrderId, AuthenticatedUser actor) {
        StoreOrderView view = findViewOrThrow(storeOrderId);
        requireReadAccess(actor, view.storeId(), view.warehouseId());
        StoreOrderOutboundStatus latest = storeOrderOutboundPort.findLatestOutboundStatus(storeOrderId).orElse(null);
        return new StoreOrderDetail(view, findStatusReason(view),
                StoreOrderProgressStage.resolve(view.status(), latest, view.hasShortage()));
    }

    @Override
    public StoreOrderDetails getStoreOrderDetails(Long storeOrderId, AuthenticatedUser actor) {
        StoreOrderView view = findViewOrThrow(storeOrderId);
        requireReadAccess(actor, view.storeId(), view.warehouseId());
        StoreOrderOutboundStatus latest = storeOrderOutboundPort.findLatestOutboundStatus(storeOrderId).orElse(null);

        List<StoreOrderStatusHistoryView> history = statusHistoryUseCase
                .findHistory(StatusHistoryEntityType.STORE_ORDER, storeOrderId).stream()
                .map(h -> new StoreOrderStatusHistoryView(
                        h.getFromStatus(), h.getToStatus(), h.getReason(), h.getChangedBy(), h.getChangedAt()))
                .toList();

        return new StoreOrderDetails(
                view.storeOrderId(), view.orderNo(), view.status(),
                StoreOrderProgressStage.resolve(view.status(), latest, view.hasShortage()),
                storeOrderQueryRepository.findLineViews(storeOrderId),
                storeOrderOutboundPort.findOutbounds(storeOrderId),
                history);
    }

    /**
     * 발주 행을 비관적 락으로 잡고 상태 확인과 변경을 한 트랜잭션에서 처리해 같은 발주에 대한
     * 동시 승인·반려·취소 중 하나만 성공하게 한다. 승인 조건은 지점과 모든 항목의 SKU가 활성인 것이다.
     */
    @Override
    @Transactional
    public StoreOrderStatusChange approveStoreOrder(Long storeOrderId, Long changedBy) {
        requireChangedBy(changedBy);
        StoreOrder order = findForUpdateOrThrow(storeOrderId);
        if (!order.isRequested()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "요청 상태의 발주만 승인할 수 있습니다. 현재 상태: " + order.getStatus());
        }

        storeAvailabilityPort.requireActive(order.getStoreId());
        List<StoreOrderLine> lines = storeOrderRepository.findLinesByStoreOrderIdForUpdate(storeOrderId);
        lines.forEach(line -> skuAvailabilityPort.requireActive(line.getSkuId()));

        StoreOrderStatus from = order.getStatus();
        order.approve();
        StoreOrder saved = storeOrderRepository.save(order);
        statusHistoryUseCase.record(StatusHistoryEntityType.STORE_ORDER, storeOrderId,
                from.name(), saved.getStatus().name(), null, changedBy);

        return new StoreOrderStatusChange(storeOrderId, saved.getOrderNo(), saved.getStatus(), null,
                updatedAtOf(saved));
    }

    @Override
    @Transactional
    public StoreOrderStatusChange rejectStoreOrder(StoreOrderRejectCommand command) {
        requireChangedBy(command.changedBy());
        String reason = normalizeReason(command.reason());
        if (reason == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "반려 사유를 입력해주세요.");
        }

        StoreOrder order = findForUpdateOrThrow(command.storeOrderId());
        if (!order.isRequested()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "요청 상태의 발주만 반려할 수 있습니다. 현재 상태: " + order.getStatus());
        }

        StoreOrderStatus from = order.getStatus();
        order.reject();
        StoreOrder saved = storeOrderRepository.save(order);
        cancelLines(command.storeOrderId());
        statusHistoryUseCase.record(StatusHistoryEntityType.STORE_ORDER, command.storeOrderId(),
                from.name(), saved.getStatus().name(), reason, command.changedBy());

        return new StoreOrderStatusChange(command.storeOrderId(), saved.getOrderNo(), saved.getStatus(), reason,
                updatedAtOf(saved));
    }

    /**
     * 승인 전(REQUESTED) 취소는 사유가 선택이고, 승인 이후(APPROVED·ASSIGNED·ON_HOLD) 취소는 사유가 필수다.
     * 피킹이 시작된 출고가 있으면 막고, 승인 이후 취소는 출고 연동 포트로 READY 출고 취소·할당 해제를 같은 트랜잭션에서 처리한다.
     * 승인 전 취소는 작성자인 점주만, 승인 이후 취소는 본사 관리자만 할 수 있다.
     */
    @Override
    @Transactional
    public StoreOrderCancelResult cancelStoreOrder(StoreOrderCancelCommand command, AuthenticatedUser actor) {
        requireChangedBy(command.changedBy());
        String reason = normalizeReason(command.reason());

        StoreOrder order = findForUpdateOrThrow(command.storeOrderId());
        // 소속을 먼저 확인해, 다른 지점 사용자에게 발주의 상태(409)가 드러나지 않게 한다. 본사는 전체 지점을 취소할 수 있다.
        actor.requireStoreAccess(order.getStoreId());
        if (order.isTerminal()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "진행 중인 발주만 취소할 수 있습니다. 현재 상태: " + order.getStatus());
        }
        boolean afterApproval = !order.isRequested();
        boolean allowed = afterApproval
                ? actor.isHqAdmin()
                : actor.isStoreOwner() && actor.userId().equals(order.getCreatedBy());
        if (!allowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (afterApproval && reason == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "승인된 발주를 취소할 때는 사유를 입력해야 합니다.");
        }
        if (storeOrderOutboundPort.existsPickingStarted(command.storeOrderId())) {
            throw new BusinessException(StoreOrderErrorCode.ORDER_IN_PICKING);
        }

        StoreOrderFulfillmentCancelResult fulfillment = afterApproval
                ? storeOrderOutboundPort.cancelFulfillment(command.storeOrderId(), command.changedBy())
                : StoreOrderFulfillmentCancelResult.NONE;

        StoreOrderStatus from = order.getStatus();
        order.cancel();
        StoreOrder saved = storeOrderRepository.save(order);
        cancelLines(command.storeOrderId());
        statusHistoryUseCase.record(StatusHistoryEntityType.STORE_ORDER, command.storeOrderId(),
                from.name(), saved.getStatus().name(), reason, command.changedBy());

        return new StoreOrderCancelResult(command.storeOrderId(), saved.getOrderNo(), saved.getStatus(), reason,
                fulfillment.releasedAllocationCount(), fulfillment.canceledOutboundCount(), updatedAtOf(saved));
    }

    /**
     * 최초 배정(APPROVED → ASSIGNED)과 재배정(ASSIGNED의 창고 변경)을 한 API로 처리한다.
     * 재배정은 사유가 필수이고, 재고 할당·취소되지 않은 출고가 남아 있으면 막는다(ON_HOLD는 재배정 불가).
     * 재배정 이력의 사유에는 이전·이후 창고를 함께 남긴다.
     */
    @Override
    @Transactional
    public StoreOrderAssignResult assignStoreOrder(StoreOrderAssignCommand command) {
        requireChangedBy(command.changedBy());
        if (command.storeOrderId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "배정할 발주를 선택해주세요.");
        }
        if (command.warehouseId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "배정할 창고를 선택해주세요.");
        }
        String reason = normalizeReason(command.reason());

        StoreOrder order = findForUpdateOrThrow(command.storeOrderId());
        StoreOrderStatus from = order.getStatus();
        if (from != StoreOrderStatus.APPROVED && from != StoreOrderStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "승인 또는 창고 배정 상태의 발주만 창고를 배정할 수 있습니다. 현재 상태: " + from);
        }
        warehouseAvailabilityPort.requireActive(command.warehouseId());

        String historyReason;
        if (from == StoreOrderStatus.APPROVED) {
            order.assign(command.warehouseId());
            historyReason = reason;
        } else {
            if (reason == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "창고를 재배정할 때는 사유를 입력해야 합니다.");
            }
            if (command.warehouseId().equals(order.getWarehouseId())) {
                throw new BusinessException(ErrorCode.CONFLICT, "현재 배정된 창고와 다른 창고를 지정해야 합니다.");
            }
            if (storeOrderOutboundPort.existsActiveFulfillment(command.storeOrderId())) {
                throw new BusinessException(StoreOrderErrorCode.ORDER_IN_FULFILLMENT);
            }
            Long previousWarehouseId = order.getWarehouseId();
            order.reassign(command.warehouseId());
            historyReason = reassignReason(previousWarehouseId, command.warehouseId(), reason);
        }

        StoreOrder saved = storeOrderRepository.save(order);
        statusHistoryUseCase.record(StatusHistoryEntityType.STORE_ORDER, command.storeOrderId(),
                from.name(), saved.getStatus().name(), historyReason, command.changedBy());

        StoreOrderView view = storeOrderQueryRepository.findView(command.storeOrderId()).orElse(null);
        return new StoreOrderAssignResult(command.storeOrderId(), saved.getOrderNo(), saved.getStatus(),
                saved.getWarehouseId(), view == null ? null : view.warehouseName(),
                view == null ? saved.getUpdatedAt() : view.updatedAt());
    }

    /** 피킹이 시작된 출고가 있으면 막는다. READY 출고나 재고 할당이 있어도 보류할 수 있고 재고·수량은 바뀌지 않는다. */
    @Override
    @Transactional
    public StoreOrderStatusChange holdStoreOrder(StoreOrderHoldCommand command, AuthenticatedUser actor) {
        requireChangedBy(command.changedBy());
        String reason = requireReason(command.reason(), "보류 사유를 입력해주세요.");

        StoreOrder order = findForUpdateOrThrow(command.storeOrderId());
        actor.requireWarehouseAccess(order.getWarehouseId());
        if (order.getStatus() != StoreOrderStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "창고 배정 상태의 발주만 출고를 보류할 수 있습니다. 현재 상태: " + order.getStatus());
        }
        if (storeOrderOutboundPort.existsPickingStarted(command.storeOrderId())) {
            throw new BusinessException(StoreOrderErrorCode.ORDER_IN_PICKING);
        }

        StoreOrderStatus from = order.getStatus();
        order.hold();
        StoreOrder saved = storeOrderRepository.save(order);
        statusHistoryUseCase.record(StatusHistoryEntityType.STORE_ORDER, command.storeOrderId(),
                from.name(), saved.getStatus().name(), reason, command.changedBy());

        return new StoreOrderStatusChange(command.storeOrderId(), saved.getOrderNo(), saved.getStatus(), reason,
                updatedAtOf(saved));
    }

    /** 재고 충분 여부는 검증하지 않는다. 배정 창고가 비활성이면 재개할 수 없다. 재개 후 statusReason은 null이다. */
    @Override
    @Transactional
    public StoreOrderStatusChange resumeStoreOrder(StoreOrderResumeCommand command, AuthenticatedUser actor) {
        requireChangedBy(command.changedBy());
        String reason = requireReason(command.reason(), "재개 사유를 입력해주세요.");

        StoreOrder order = findForUpdateOrThrow(command.storeOrderId());
        actor.requireWarehouseAccess(order.getWarehouseId());
        if (order.getStatus() != StoreOrderStatus.ON_HOLD) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "출고 보류 상태의 발주만 재개할 수 있습니다. 현재 상태: " + order.getStatus());
        }
        warehouseAvailabilityPort.requireActive(order.getWarehouseId());

        StoreOrderStatus from = order.getStatus();
        order.resume();
        StoreOrder saved = storeOrderRepository.save(order);
        statusHistoryUseCase.record(StatusHistoryEntityType.STORE_ORDER, command.storeOrderId(),
                from.name(), saved.getStatus().name(), reason, command.changedBy());

        return new StoreOrderStatusChange(command.storeOrderId(), saved.getOrderNo(), saved.getStatus(), null,
                updatedAtOf(saved));
    }

    /**
     * 진행 중 출고가 없고 부족한 항목이 있을 때만 ASSIGNED → COMPLETED로 종결한다.
     * 출고에 묶이지 않고 남은 ALLOCATED 재고 할당은 출고 연동 포트로 같은 트랜잭션에서 모두 해제하고
     * (재고 행·발주 항목의 allocated_quantity 감소), 부족 수량을 계산해 돌려준다. 출고·항목 상태는 바꾸지 않는다.
     * 남은 수량은 자동 재발주하지 않는다.
     */
    @Override
    @Transactional
    public StoreOrderCompletePartialResult completePartialStoreOrder(StoreOrderCompletePartialCommand command, AuthenticatedUser actor) {
        requireChangedBy(command.changedBy());
        String reason = requireReason(command.reason(), "종결 사유를 입력해주세요.");

        StoreOrder order = findForUpdateOrThrow(command.storeOrderId());
        actor.requireWarehouseAccess(order.getWarehouseId());
        if (order.getStatus() != StoreOrderStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "창고 배정 상태의 발주만 부분 출고로 종결할 수 있습니다. 현재 상태: " + order.getStatus());
        }
        if (storeOrderOutboundPort.existsInProgressOutbound(command.storeOrderId())) {
            throw new BusinessException(StoreOrderErrorCode.OUTBOUND_IN_PROGRESS);
        }
        // 락 순서(할당 → 발주 항목)를 지키려고 항목은 잠그지 않고 읽는다. 헤더 잠금과 진행 중 출고 없음으로 수량은 고정이다.
        List<StoreOrderLine> lines = storeOrderRepository.findLinesByStoreOrderId(command.storeOrderId());
        if (lines.stream().allMatch(StoreOrderLine::isFulfilled)) {
            throw new BusinessException(StoreOrderErrorCode.NO_SHORTAGE);
        }
        int releasedAllocationCount = storeOrderOutboundPort
                .releaseUnlinkedAllocations(command.storeOrderId(), command.changedBy());

        StoreOrderStatus from = order.getStatus();
        order.complete();
        StoreOrder saved = storeOrderRepository.save(order);
        statusHistoryUseCase.record(StatusHistoryEntityType.STORE_ORDER, command.storeOrderId(),
                from.name(), saved.getStatus().name(), reason, command.changedBy());

        List<StoreOrderCompletePartialResult.Item> items = storeOrderQueryRepository
                .findLineViews(command.storeOrderId()).stream()
                .map(line -> new StoreOrderCompletePartialResult.Item(line.storeOrderLineId(), line.skuCode(),
                        line.requestedQuantity(), line.shippedQuantity(), line.remainingQuantity()))
                .toList();
        return new StoreOrderCompletePartialResult(command.storeOrderId(), saved.getOrderNo(), saved.getStatus(),
                reason, items, releasedAllocationCount, updatedAtOf(saved));
    }

    /**
     * 재배정 이력의 사유. 이전·이후 창고를 앞에 붙이고, StatusHistory 사유 길이 제한(500자)을 넘으면
     * 사용자 사유 끝을 줄인다.
     */
    private String reassignReason(Long previousWarehouseId, Long newWarehouseId, String reason) {
        String prefix = "창고 변경 " + previousWarehouseId + " → " + newWarehouseId + ": ";
        int available = REASON_MAX_LENGTH - prefix.length();
        String body = reason.length() <= available ? reason : reason.substring(0, available - 1) + "…";
        return prefix + body;
    }

    /** 사유가 필수인 전이용. 비어 있으면 400 VALIDATION_ERROR. */
    private String requireReason(String reason, String message) {
        String normalized = normalizeReason(reason);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
        return normalized;
    }

    /** 점주는 담당 지점의 발주, 창고 관리자는 담당 창고에 배정된 발주(미배정이면 불가), 본사는 전체를 볼 수 있다. */
    private static void requireReadAccess(AuthenticatedUser actor, Long storeId, Long warehouseId) {
        if (actor.isStoreOwner()) {
            actor.requireStoreAccess(storeId);
        } else {
            actor.requireWarehouseAccess(warehouseId);
        }
    }

    private void requireChangedBy(Long changedBy) {
        if (changedBy == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "처리 사용자는 필수입니다.");
        }
    }

    /** 공백만 있으면 null, 500자를 넘으면 400. 앞뒤 공백은 제거한다. */
    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String trimmed = reason.strip();
        if (trimmed.length() > REASON_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "사유는 " + REASON_MAX_LENGTH + "자 이하여야 합니다.");
        }
        return trimmed;
    }

    /** 발주 취소·반려에 따라 항목을 모두 CANCELED로 바꾼다. 항목 행도 잠그고 처리한다. */
    private void cancelLines(Long storeOrderId) {
        List<StoreOrderLine> lines = storeOrderRepository.findLinesByStoreOrderIdForUpdate(storeOrderId);
        lines.forEach(StoreOrderLine::cancel);
        storeOrderRepository.saveLines(lines);
    }

    /**
     * 저장 결과의 updatedAt은 flush 전이라 비어 있을 수 있어, 조회 쿼리(JPQL 실행 전 flush)로 확정된 값을 읽는다.
     */
    private LocalDateTime updatedAtOf(StoreOrder saved) {
        return storeOrderQueryRepository.findView(saved.getStoreOrderId())
                .map(StoreOrderView::updatedAt)
                .orElse(saved.getUpdatedAt());
    }

    private StoreOrder findForUpdateOrThrow(Long storeOrderId) {
        return storeOrderRepository.findByIdForUpdate(storeOrderId)
                .orElseThrow(() -> new BusinessException(StoreOrderErrorCode.STORE_ORDER_NOT_FOUND));
    }

    private void validateRegister(StoreOrderRegisterCommand command) {
        if (command.storeId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "지점을 선택해주세요.");
        }
        if (command.createdBy() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청 사용자는 필수입니다.");
        }
        if (command.lines() == null || command.lines().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "발주 항목을 1개 이상 입력해주세요.");
        }
        Set<Long> skuIds = new HashSet<>();
        for (StoreOrderRegisterCommand.Line line : command.lines()) {
            if (line.skuId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SKU는 필수입니다.");
            }
            if (line.requestedQuantity() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "발주 수량은 1 이상이어야 합니다.");
            }
            if (!skuIds.add(line.skuId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "같은 SKU를 중복해서 발주할 수 없습니다. skuId=" + line.skuId());
            }
        }
        if (command.requestedDeliveryAt() != null && !command.requestedDeliveryAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청 배송 일시는 현재 시각 이후여야 합니다.");
        }
        if (command.note() != null && command.note().length() > NOTE_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "비고는 " + NOTE_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }

    /**
     * SO-YYYYMMDD-일련번호(4자리). 당일 발주 개수 + 1로 채번하며, 동시 등록으로 번호가 겹치면
     * UNIQUE 제약이 409 DUPLICATE_STORE_ORDER_NO로 응답한다.
     */
    private String nextOrderNo(LocalDate today) {
        String prefix = NO_PREFIX + today.format(NO_DATE_FORMAT) + "-";
        long sequence = storeOrderRepository.countByOrderNoPrefix(prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    /** 반려·취소·보류 상태에서만 이력의 사유를 읽는다. 그 외 상태(재개 후 ASSIGNED 포함)는 null. */
    private String findStatusReason(StoreOrderView view) {
        if (!REASON_STATUSES.contains(view.status())) {
            return null;
        }
        return statusHistoryUseCase
                .findStatusReason(StatusHistoryEntityType.STORE_ORDER, view.storeOrderId(), view.status().name())
                .orElse(null);
    }

    private StoreOrderView findViewOrThrow(Long storeOrderId) {
        return storeOrderQueryRepository.findView(storeOrderId)
                .orElseThrow(() -> new BusinessException(StoreOrderErrorCode.STORE_ORDER_NOT_FOUND));
    }
}

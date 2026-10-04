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

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.storeorder.application.port.in.StoreOrderUseCase;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderRegisterCommand;
import com.kb.wms.storeorder.application.port.in.query.StoreOrderSearchCondition;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetail;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetails;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderListItem;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderStatusHistoryView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderSummary;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderView;
import com.kb.wms.storeorder.application.port.out.SkuSupplyPricePort;
import com.kb.wms.storeorder.application.port.out.StoreAvailabilityPort;
import com.kb.wms.storeorder.application.port.out.StoreOrderOutboundPort;
import com.kb.wms.storeorder.application.port.out.StoreOrderQueryRepository;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.application.port.out.WarehouseExistencePort;
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

    /** 응답의 statusReason을 내려주는 상태. 재개 후 ASSIGNED 등 사유가 필요 없는 상태에서는 null이다. */
    private static final Set<StoreOrderStatus> REASON_STATUSES =
            Set.of(StoreOrderStatus.REJECTED, StoreOrderStatus.CANCELED, StoreOrderStatus.ON_HOLD);

    private final StoreOrderRepository storeOrderRepository;
    private final StoreOrderQueryRepository storeOrderQueryRepository;
    private final StoreOrderOutboundPort storeOrderOutboundPort;
    private final StoreAvailabilityPort storeAvailabilityPort;
    private final WarehouseExistencePort warehouseExistencePort;
    private final SkuSupplyPricePort skuSupplyPricePort;
    private final StatusHistoryUseCase statusHistoryUseCase;

    @Override
    @Transactional
    public Long registerStoreOrder(StoreOrderRegisterCommand command) {
        validateRegister(command);

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
        if (condition.requestedFrom() != null && condition.requestedTo() != null
                && condition.requestedFrom().isAfter(condition.requestedTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청 시작 일시는 종료 일시보다 늦을 수 없습니다.");
        }
        if (condition.storeId() != null) {
            storeAvailabilityPort.requireExists(condition.storeId());
        }
        if (condition.warehouseId() != null) {
            warehouseExistencePort.requireExists(condition.warehouseId());
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
    public StoreOrderDetail getStoreOrder(Long storeOrderId) {
        StoreOrderView view = findViewOrThrow(storeOrderId);
        StoreOrderOutboundStatus latest = storeOrderOutboundPort.findLatestOutboundStatus(storeOrderId).orElse(null);
        return new StoreOrderDetail(view, findStatusReason(view),
                StoreOrderProgressStage.resolve(view.status(), latest, view.hasShortage()));
    }

    @Override
    public StoreOrderDetails getStoreOrderDetails(Long storeOrderId) {
        StoreOrderView view = findViewOrThrow(storeOrderId);
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

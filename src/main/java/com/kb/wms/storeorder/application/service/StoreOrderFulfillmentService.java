package com.kb.wms.storeorder.application.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.storeorder.application.port.in.StoreOrderFulfillmentUseCase;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderLinePickedCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderLineQuantityCommand;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.storeorder.exception.StoreOrderErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 출고 도메인이 쓰는 지점 발주 연동 서비스. {@code StoreOrderService}와 분리해 빈 순환 의존을 피한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreOrderFulfillmentService implements StoreOrderFulfillmentUseCase {

    private final StoreOrderRepository storeOrderRepository;
    private final StatusHistoryUseCase statusHistoryUseCase;

    @Override
    public StoreOrder getOrder(Long storeOrderId) {
        return storeOrderRepository.findById(storeOrderId)
                .orElseThrow(() -> new BusinessException(StoreOrderErrorCode.STORE_ORDER_NOT_FOUND));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public StoreOrder getOrderForUpdate(Long storeOrderId) {
        return storeOrderRepository.findByIdForUpdate(storeOrderId)
                .orElseThrow(() -> new BusinessException(StoreOrderErrorCode.STORE_ORDER_NOT_FOUND));
    }

    @Override
    public List<StoreOrderLine> getLines(Long storeOrderId) {
        return storeOrderRepository.findLinesByStoreOrderId(storeOrderId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<StoreOrderLine> getLinesForUpdate(Long storeOrderId) {
        return storeOrderRepository.findLinesByStoreOrderIdForUpdate(storeOrderId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void increaseAllocated(List<StoreOrderLineQuantityCommand> commands) {
        Map<Long, StoreOrderLine> lines = lockLines(commands.stream().map(StoreOrderLineQuantityCommand::storeOrderLineId).toList());
        commands.forEach(c -> lines.get(c.storeOrderLineId()).increaseAllocated(c.quantity()));
        storeOrderRepository.saveLines(List.copyOf(lines.values()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void decreaseAllocated(List<StoreOrderLineQuantityCommand> commands) {
        Map<Long, StoreOrderLine> lines = lockLines(commands.stream().map(StoreOrderLineQuantityCommand::storeOrderLineId).toList());
        commands.forEach(c -> lines.get(c.storeOrderLineId()).decreaseAllocated(c.quantity()));
        storeOrderRepository.saveLines(List.copyOf(lines.values()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyPicked(List<StoreOrderLinePickedCommand> commands) {
        Map<Long, StoreOrderLine> lines = lockLines(commands.stream().map(StoreOrderLinePickedCommand::storeOrderLineId).toList());
        commands.forEach(c -> lines.get(c.storeOrderLineId()).applyPicked(c.allocatedQuantity(), c.pickedQuantity()));
        storeOrderRepository.saveLines(List.copyOf(lines.values()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void refreshLineStatuses(Collection<Long> storeOrderLineIds) {
        Map<Long, StoreOrderLine> lines = lockLines(storeOrderLineIds);
        lines.values().forEach(StoreOrderLine::refreshStatusByShipped);
        storeOrderRepository.saveLines(List.copyOf(lines.values()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public StoreOrder completeIfFulfilled(Long storeOrderId, Long changedBy) {
        StoreOrder order = getOrderForUpdate(storeOrderId);
        if (order.getStatus() != StoreOrderStatus.ASSIGNED) {
            return order;
        }
        List<StoreOrderLine> lines = storeOrderRepository.findLinesByStoreOrderId(storeOrderId);
        if (lines.isEmpty() || !lines.stream().allMatch(StoreOrderLine::isFulfilled)) {
            return order;
        }
        StoreOrderStatus from = order.getStatus();
        order.complete();
        StoreOrder saved = storeOrderRepository.save(order);
        statusHistoryUseCase.record(StatusHistoryEntityType.STORE_ORDER, storeOrderId,
                from.name(), saved.getStatus().name(), null, changedBy);
        return saved;
    }

    /** 항목을 ID 오름차순으로 잠가 ID → 항목 맵으로 돌려준다. 없는 항목이 있으면 404가 아니라 호출 오류다. */
    private Map<Long, StoreOrderLine> lockLines(Collection<Long> storeOrderLineIds) {
        List<Long> ids = storeOrderLineIds.stream().distinct().sorted().toList();
        Map<Long, StoreOrderLine> lines = storeOrderRepository.findLinesByIdsForUpdate(ids).stream()
                .collect(Collectors.toMap(StoreOrderLine::getStoreOrderLineId, Function.identity(),
                        (a, b) -> a, java.util.LinkedHashMap::new));
        if (lines.size() != ids.size()) {
            throw new IllegalArgumentException("존재하지 않는 발주 항목이 포함되어 있습니다. 요청: " + ids);
        }
        return lines;
    }
}

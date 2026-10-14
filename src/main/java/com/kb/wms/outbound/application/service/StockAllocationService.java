package com.kb.wms.outbound.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.outbound.application.port.in.StockAllocationUseCase;
import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;
import com.kb.wms.outbound.application.port.in.command.StockAllocationReleaseCommand;
import com.kb.wms.outbound.application.port.in.query.StockAllocationSearchCondition;
import com.kb.wms.outbound.application.port.in.result.FefoStockCandidate;
import com.kb.wms.outbound.application.port.in.result.StockAllocateResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationDetail;
import com.kb.wms.outbound.application.port.in.result.StockAllocationReleaseResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationShortage;
import com.kb.wms.outbound.application.port.in.result.StockAllocationSummary;
import com.kb.wms.outbound.application.port.in.result.StockAllocationView;
import com.kb.wms.outbound.application.port.out.OutboundQueryRepository;
import com.kb.wms.outbound.application.port.out.OutboundStockPort.StockQuantity;
import com.kb.wms.outbound.application.port.out.OutboundStockPort.StockState;
import com.kb.wms.outbound.application.port.out.OutboundStockPort;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.application.port.out.StoreOrderPort.LineQuantity;
import com.kb.wms.outbound.application.port.out.StoreOrderPort.LineView;
import com.kb.wms.outbound.application.port.out.StoreOrderPort.OrderView;
import com.kb.wms.outbound.application.port.out.StoreOrderPort;
import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.domain.enums.OutboundHistoryEntityType;
import com.kb.wms.outbound.domain.enums.StoreOrderState;
import com.kb.wms.outbound.exception.OutboundErrorCode;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 재고 할당·해제 서비스(서비스 A). 상태 확인과 변경은 발주 헤더를 잠근 상태에서 한 트랜잭션으로 처리하고,
 * 재고 행 잠금과 수량 변경은 재고 서비스에 맡긴다. 전역 락 순서와 구역 잠금은 성능 개선 단계에서 다룬다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockAllocationService implements StockAllocationUseCase {

    static final int REASON_MAX_LENGTH = 500;

    private final StockAllocationRepository stockAllocationRepository;
    private final OutboundQueryRepository outboundQueryRepository;
    private final OutboundStockPort outboundStockPort;
    private final StoreOrderPort storeOrderPort;
    private final StatusHistoryUseCase statusHistoryUseCase;

    @Override
    @Transactional
    public StockAllocateResult allocate(StockAllocateCommand command, AuthenticatedUser actor) {
        requireUser(command.userId());
        if (command.storeOrderId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "할당할 발주를 선택해주세요.");
        }

        OrderView order = storeOrderPort.getOrderForUpdate(command.storeOrderId());
        actor.requireWarehouseAccess(order.warehouseId());
        if (order.state() != StoreOrderState.ASSIGNED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "배정된 발주만 재고를 할당할 수 있습니다. 현재 상태: " + order.state());
        }

        List<LineView> lines = storeOrderPort.getLinesForUpdate(order.storeOrderId());
        List<LineView> targets = selectTargets(lines, command.skuIds()).stream()
                .filter(line -> line.unallocatedQuantity() > 0)
                .toList();
        if (targets.isEmpty()) {
            throw new BusinessException(OutboundErrorCode.ALREADY_ALLOCATED);
        }

        FefoPlacement fefo = placeByFefo(order.warehouseId(), targets);
        List<Placement> placements = fefo.placements();
        if (placements.isEmpty()) {
            throw new BusinessException(OutboundErrorCode.INSUFFICIENT_STOCK,
                    OutboundErrorCode.INSUFFICIENT_STOCK.getDefaultMessage() + " ("
                            + fefo.shortages().stream()
                                    .map(s -> "skuId=" + s.skuId() + " 요청 " + s.requestedQuantity()
                                            + ", 가용 " + s.allocatedQuantity())
                                    .collect(Collectors.joining("; ")) + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        List<StockAllocation> saved = stockAllocationRepository.saveAll(placements.stream()
                .map(p -> StockAllocation.allocate(p.storeOrderLineId(), p.inventoryLotId(), p.quantity(),
                        command.userId(), now))
                .toList());

        outboundStockPort.allocate(sumBy(placements, Placement::inventoryLotId).entrySet().stream()
                .map(e -> new StockQuantity(e.getKey(), e.getValue()))
                .toList());
        storeOrderPort.increaseAllocated(sumBy(placements, Placement::storeOrderLineId).entrySet().stream()
                .map(e -> new LineQuantity(e.getKey(), e.getValue()))
                .toList());

        for (StockAllocation allocation : saved) {
            statusHistoryUseCase.record(OutboundHistoryEntityType.STOCK_ALLOCATION.name(), allocation.getAllocationId(),
                    null, AllocationStatus.ALLOCATED.name(), null, command.userId());
        }

        List<StockAllocationSummary> items = outboundQueryRepository.findAllocationSummaries(
                saved.stream().map(StockAllocation::getAllocationId).toList());
        return new StockAllocateResult(order.storeOrderId(), order.orderNo(), items, fefo.shortages());
    }

    @Override
    @Transactional
    public StockAllocationReleaseResult release(StockAllocationReleaseCommand command, AuthenticatedUser actor) {
        requireUser(command.userId());
        String reason = normalizeReason(command.reason());
        if (reason == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "할당 해제 사유를 입력해주세요.");
        }
        if (reason.length() > REASON_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "해제 사유는 " + REASON_MAX_LENGTH + "자 이하여야 합니다.");
        }

        // 발주 → 할당 순으로 잠그기 위해, 잠그지 않는 조회로 발주를 먼저 찾는다.
        StockAllocationView view = outboundQueryRepository.findAllocationView(command.allocationId())
                .orElseThrow(() -> new BusinessException(OutboundErrorCode.ALLOCATION_NOT_FOUND));
        OrderView order = storeOrderPort.getOrderForUpdate(view.storeOrderId());
        actor.requireWarehouseAccess(order.warehouseId());
        StockAllocation allocation = stockAllocationRepository.findByIdForUpdate(command.allocationId())
                .orElseThrow(() -> new BusinessException(OutboundErrorCode.ALLOCATION_NOT_FOUND));

        if (allocation.getStatus() != AllocationStatus.ALLOCATED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "할당 상태의 재고 할당만 해제할 수 있습니다. 현재 상태: " + allocation.getStatus());
        }
        if (stockAllocationRepository.isLinkedToActiveOutbound(allocation.getAllocationId())) {
            throw new BusinessException(OutboundErrorCode.ALLOCATION_IN_OUTBOUND);
        }

        allocation.release(LocalDateTime.now());
        StockAllocation saved = stockAllocationRepository.save(allocation);

        List<StockState> states = outboundStockPort.release(
                List.of(new StockQuantity(saved.getInventoryLotId(), saved.getAllocatedQuantity())));
        storeOrderPort.decreaseAllocated(
                List.of(new LineQuantity(saved.getStoreOrderLineId(), saved.getAllocatedQuantity())));
        statusHistoryUseCase.record(OutboundHistoryEntityType.STOCK_ALLOCATION.name(), saved.getAllocationId(),
                AllocationStatus.ALLOCATED.name(), AllocationStatus.RELEASED.name(), reason, command.userId());

        StockState state = states.get(0);
        return new StockAllocationReleaseResult(saved.getAllocationId(), saved.getStatus(),
                saved.getAllocatedQuantity(), saved.getReleasedAt(), state.inventoryLotId(),
                state.onHandQuantity(), state.allocatedQuantity(), state.availableQuantity());
    }

    @Override
    public List<StockAllocationSummary> searchAllocations(StockAllocationSearchCondition condition,
                                                          AuthenticatedUser actor) {
        List<Long> scope = actor.warehouseScope(condition.warehouseId());
        return outboundQueryRepository.searchAllocations(new StockAllocationSearchCondition(
                condition.storeOrderId(), condition.warehouseId(), condition.skuId(), condition.status(),
                condition.keyword(), scope));
    }

    @Override
    public StockAllocationDetail getAllocation(Long allocationId, AuthenticatedUser actor) {
        StockAllocationView view = outboundQueryRepository.findAllocationView(allocationId)
                .orElseThrow(() -> new BusinessException(OutboundErrorCode.ALLOCATION_NOT_FOUND));
        actor.requireWarehouseAccess(view.warehouseId());
        Long outboundId = outboundQueryRepository.findActiveOutboundIdByAllocationId(allocationId).orElse(null);
        return new StockAllocationDetail(view, outboundId);
    }

    /**
     * 요청한 SKU의 발주 항목만 고른다. skuIds가 null이면 발주 전체다.
     * 한 발주 안에서 SKU는 중복될 수 없으므로 SKU로 항목이 하나로 정해진다.
     */
    private List<LineView> selectTargets(List<LineView> lines, List<Long> skuIds) {
        if (skuIds == null) {
            return lines;
        }
        if (skuIds.isEmpty() || skuIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "할당할 SKU를 하나 이상 지정해주세요.");
        }
        Set<Long> requested = new HashSet<>(skuIds);
        Set<Long> inOrder = lines.stream().map(LineView::skuId).collect(Collectors.toSet());
        List<Long> unknown = requested.stream().filter(id -> !inOrder.contains(id)).sorted().toList();
        if (!unknown.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "발주에 없는 SKU입니다. skuId=" + unknown);
        }
        return lines.stream().filter(line -> requested.contains(line.skuId())).toList();
    }

    /**
     * 항목별 잔여 수량을 FEFO 후보 행에 순서대로 채운다. 한 항목이 여러 행에 나뉠 수 있다.
     * 재고가 모자란 항목은 가용 수량만큼만 채우고 모자란 수량을 shortages에 담는다.
     * 아무것도 못 채웠을 때의 거절은 호출한 쪽이 한다.
     */
    private FefoPlacement placeByFefo(Long warehouseId, List<LineView> targets) {
        Map<Long, List<FefoStockCandidate>> candidatesBySku = outboundQueryRepository
                .findFefoCandidates(warehouseId, targets.stream().map(LineView::skuId).distinct().toList())
                .stream()
                .collect(Collectors.groupingBy(FefoStockCandidate::skuId, LinkedHashMap::new, Collectors.toList()));

        List<Placement> placements = new ArrayList<>();
        List<StockAllocationShortage> shortages = new ArrayList<>();
        Map<Long, Long> availableByLot = new HashMap<>();
        for (LineView line : targets) {
            long remaining = line.unallocatedQuantity();
            long requested = remaining;
            for (FefoStockCandidate candidate : candidatesBySku.getOrDefault(line.skuId(), List.of())) {
                if (remaining == 0) {
                    break;
                }
                long available = availableByLot.getOrDefault(candidate.inventoryLotId(), candidate.availableQuantity());
                long take = Math.min(available, remaining);
                if (take <= 0) {
                    continue;
                }
                placements.add(new Placement(line.storeOrderLineId(), candidate.inventoryLotId(), take));
                availableByLot.put(candidate.inventoryLotId(), available - take);
                remaining -= take;
            }
            if (remaining > 0) {
                shortages.add(new StockAllocationShortage(line.skuId(), requested, requested - remaining, remaining));
            }
        }
        return new FefoPlacement(placements, shortages);
    }

    /** 키 오름차순으로 수량을 합산한다. 재고 행·발주 항목을 ID 오름차순으로 잠그기 위한 순서다. */
    private Map<Long, Long> sumBy(List<Placement> placements, java.util.function.Function<Placement, Long> key) {
        Map<Long, Long> sums = new TreeMap<>();
        placements.forEach(p -> sums.merge(key.apply(p), p.quantity(), Long::sum));
        return sums;
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청 사용자는 필수입니다.");
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Placement(Long storeOrderLineId, Long inventoryLotId, long quantity) {
    }

    private record FefoPlacement(List<Placement> placements, List<StockAllocationShortage> shortages) {
    }
}

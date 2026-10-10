package com.kb.wms.inbound.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.inbound.application.port.in.InboundCompleteUseCase;
import com.kb.wms.inbound.application.port.in.result.InboundCompleteResult;
import com.kb.wms.inbound.application.port.in.result.InboundCompleteResult.PurchaseOrderLineProgress;
import com.kb.wms.inbound.application.port.in.result.InboundCompleteResult.ReflectedInventory;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.InboundStockPort;
import com.kb.wms.inbound.application.port.out.InboundStockPort.ReceivedStock;
import com.kb.wms.inbound.application.port.out.InboundStockPort.StockReceipt;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.InboundLine;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.exception.InboundErrorCode;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 입고 완료. 재고 반영부터 입고 상태 전환까지 한 트랜잭션이다.
 *
 * <p>잠금 순서: 입고 행 → 발주 행 → 발주 항목 행 → (재고 도메인) 구역 행 → 재고 행.
 * 역할은 SecurityConfig가, 담당 창고 범위는 이 서비스가 입고 행을 잠근 직후 검사한다(ADR-012).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboundCompleteService implements InboundCompleteUseCase {

    private final InboundRepository inboundRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InboundStockPort inboundStockPort;
    private final StatusHistoryUseCase statusHistoryUseCase;

    @Override
    @Transactional
    public InboundCompleteResult completeInbound(Long inboundId, AuthenticatedUser actor) {
        Long userId = actor.userId();

        Inbound inbound = inboundRepository.findByIdForUpdate(inboundId)
                .orElseThrow(() -> new BusinessException(InboundErrorCode.INBOUND_NOT_FOUND));
        actor.requireWarehouseAccess(inbound.getWarehouseId());
        if (!inbound.isInspecting()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "검수 중 상태의 입고만 완료 처리할 수 있습니다. 현재 상태: " + inbound.getStatus());
        }

        List<InboundLine> lines = inboundRepository.findLinesByInboundId(inboundId);
        if (lines.isEmpty()) {
            throw new BusinessException(InboundErrorCode.INBOUND_HAS_NO_LINES);
        }
        if (lines.stream().anyMatch(line -> !line.hasRequiredSections())) {
            throw new BusinessException(InboundErrorCode.SECTION_NOT_ASSIGNED);
        }

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdForUpdate(inbound.getPurchaseOrderId())
                .orElseThrow(() -> new BusinessException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND));
        if (!purchaseOrder.isConfirmed()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "확정 상태의 발주에 대한 입고만 완료 처리할 수 있습니다. 발주 상태: " + purchaseOrder.getStatus());
        }
        List<PurchaseOrderLine> purchaseOrderLines =
                purchaseOrderRepository.findLinesByPurchaseOrderIdForUpdate(purchaseOrder.getPurchaseOrderId());

        List<ReflectedInventory> inventory = reflectStock(inboundId, userId, lines);
        List<PurchaseOrderLineProgress> progress = accumulateReceived(lines, purchaseOrderLines);

        if (purchaseOrderLines.stream().allMatch(PurchaseOrderLine::isCompleted)) {
            PurchaseOrderStatus purchaseOrderFrom = purchaseOrder.getStatus();
            purchaseOrder.complete();
            purchaseOrder = purchaseOrderRepository.save(purchaseOrder);
            // 마지막 입고 완료로 발주가 자동 완료되는 전이도 이력에 남기고, 처리자는 입고를 완료한 사용자로 한다.
            statusHistoryUseCase.record(StatusHistoryEntityType.PURCHASE_ORDER, purchaseOrder.getPurchaseOrderId(),
                    purchaseOrderFrom.name(), purchaseOrder.getStatus().name(), null, userId);
        }

        inbound.complete(userId, LocalDateTime.now());
        Inbound saved = inboundRepository.save(inbound);
        statusHistoryUseCase.record(StatusHistoryEntityType.INBOUND, inboundId,
                InboundStatus.INSPECTING.name(), saved.getStatus().name(), null, userId);

        return new InboundCompleteResult(
                saved, inventory, purchaseOrder.getPurchaseOrderId(), purchaseOrder.getStatus(), progress);
    }

    /** 검수 항목의 합격 수량은 합격 구역 가용 재고로, 불량 수량은 불량 구역 불량 재고로 반영한다. */
    private List<ReflectedInventory> reflectStock(Long inboundId, Long userId, List<InboundLine> lines) {
        List<StockReceipt> receipts = new ArrayList<>();
        for (InboundLine line : lines) {
            if (line.getAcceptedQuantity() > 0) {
                receipts.add(new StockReceipt(
                        line.getAcceptedSectionId(), line.getLotId(), false, line.getAcceptedQuantity()));
            }
            if (line.getDefectiveQuantity() > 0) {
                receipts.add(new StockReceipt(
                        line.getDefectSectionId(), line.getLotId(), true, line.getDefectiveQuantity()));
            }
        }

        Map<String, Long> inventoryLotIds = new HashMap<>();
        for (ReceivedStock received : inboundStockPort.receive(inboundId, userId, receipts)) {
            inventoryLotIds.put(stockKey(received.sectionId(), received.lotId(), received.defective()),
                    received.inventoryLotId());
        }

        List<ReflectedInventory> result = new ArrayList<>(lines.size());
        for (InboundLine line : lines) {
            Long acceptedId = line.getAcceptedQuantity() > 0
                    ? inventoryLotIds.get(stockKey(line.getAcceptedSectionId(), line.getLotId(), false)) : null;
            Long defectiveId = line.getDefectiveQuantity() > 0
                    ? inventoryLotIds.get(stockKey(line.getDefectSectionId(), line.getLotId(), true)) : null;
            result.add(new ReflectedInventory(
                    line.getInboundLineId(), acceptedId, line.getAcceptedQuantity(),
                    defectiveId, line.getDefectiveQuantity()));
        }
        return result;
    }

    /** 발주 항목별로 이 입고의 입고 수량(합격 + 불량) 합계를 누적하고 상태를 갱신한다. */
    private List<PurchaseOrderLineProgress> accumulateReceived(
            List<InboundLine> lines, List<PurchaseOrderLine> purchaseOrderLines) {
        Map<Long, PurchaseOrderLine> byId = new HashMap<>();
        purchaseOrderLines.forEach(line -> byId.put(line.getPurchaseOrderLineId(), line));

        Map<Long, Long> totals = new LinkedHashMap<>();
        for (InboundLine line : lines) {
            totals.merge(line.getPurchaseOrderLineId(), line.getReceivedQuantity(), Long::sum);
        }

        List<PurchaseOrderLine> updated = new ArrayList<>(totals.size());
        totals.forEach((purchaseOrderLineId, total) -> {
            PurchaseOrderLine purchaseOrderLine = byId.get(purchaseOrderLineId);
            if (purchaseOrderLine == null) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "검수 항목의 발주 항목이 입고의 발주에 없습니다. purchaseOrderLineId=" + purchaseOrderLineId);
            }
            if (total > purchaseOrderLine.remainingQuantity()) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "입고 수량이 발주 잔여 수량을 초과합니다. purchaseOrderLineId=" + purchaseOrderLineId
                                + ", 잔여=" + purchaseOrderLine.remainingQuantity() + ", 입고=" + total);
            }
            purchaseOrderLine.receive(total);
            updated.add(purchaseOrderLine);
        });

        return purchaseOrderRepository.saveLines(updated).stream()
                .map(line -> new PurchaseOrderLineProgress(
                        line.getPurchaseOrderLineId(), line.getExpectedQuantity(),
                        line.getReceivedQuantity(), line.getStatus()))
                .toList();
    }

    private static String stockKey(Long sectionId, Long lotId, boolean defective) {
        return sectionId + "/" + lotId + "/" + defective;
    }
}

package com.kb.wms.inbound.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import com.kb.wms.inbound.application.port.in.InboundInspectUseCase;
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
import com.kb.wms.inbound.exception.InboundErrorCode;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 입고 검수. 호출마다 검수 항목 전체를 교체하고, 항목마다 로트를 찾거나 만든다(ADR-004).
 * 재고·구역 사용량·발주 항목은 바꾸지 않는다(완료 처리에서 반영).
 *
 * <p>역할은 SecurityConfig가, 담당 창고 범위는 이 서비스가 입고 행을 잠근 직후 검사한다(ADR-012).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboundInspectService implements InboundInspectUseCase {

    private static final int LOT_NUMBER_MAX_LENGTH = 100;
    private static final int PRICE_CHANGE_REASON_MAX_LENGTH = 500;
    private static final int INSPECTION_NOTE_MAX_LENGTH = 1000;
    private static final int PRICE_MAX_SCALE = 2;

    private final InboundRepository inboundRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final LotPort lotPort;
    private final InboundSectionPort inboundSectionPort;
    private final StatusHistoryUseCase statusHistoryUseCase;

    @Override
    @Transactional
    public Inbound inspectInbound(Long inboundId, InboundInspectCommand command, AuthenticatedUser actor) {
        validateRequest(command);

        Inbound inbound = inboundRepository.findByIdForUpdate(inboundId)
                .orElseThrow(() -> new BusinessException(InboundErrorCode.INBOUND_NOT_FOUND));
        actor.requireWarehouseAccess(inbound.getWarehouseId());
        if (!inbound.isInspectable()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "도착 또는 검수 중 상태의 입고만 검수할 수 있습니다. 현재 상태: " + inbound.getStatus());
        }

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(inbound.getPurchaseOrderId())
                .orElseThrow(() -> new BusinessException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND));
        Map<Long, PurchaseOrderLine> purchaseOrderLines = resolvePurchaseOrderLines(inbound, command.lines());

        validatePrices(purchaseOrderLines, command.lines());
        validateRemainingQuantities(purchaseOrderLines, command.lines());
        validateSections(inbound, command.lines());

        LocalDateTime now = LocalDateTime.now();
        List<InboundLine> lines = new ArrayList<>(command.lines().size());
        for (InboundInspectCommand.Line line : command.lines()) {
            PurchaseOrderLine purchaseOrderLine = purchaseOrderLines.get(line.purchaseOrderLineId());
            Long lotId = lotPort.findOrRegisterLot(
                    purchaseOrderLine.getSkuId(), purchaseOrder.getSupplierId(), line.lotNumber(),
                    line.manufacturedDate(), line.expiryDate(), line.receivedUnitPrice());
            lines.add(InboundLine.register(
                    inbound.getInboundId(), line.purchaseOrderLineId(), lotId,
                    line.acceptedSectionId(), line.defectSectionId(),
                    line.receivedQuantity(), line.acceptedQuantity(), line.defectiveQuantity(),
                    line.receivedUnitPrice(), line.priceChangeReason(), line.inspectionNote(),
                    now, command.userId()));
        }

        InboundStatus from = inbound.getStatus();
        inbound.inspect();
        Inbound saved = inboundRepository.save(inbound);
        inboundRepository.deleteLinesByInboundId(saved.getInboundId());
        inboundRepository.saveLines(lines);
        // 검수 중 상태에서 항목을 다시 저장하는 호출은 상태가 바뀌지 않으므로 이력을 남기지 않는다.
        if (from != saved.getStatus()) {
            statusHistoryUseCase.record(StatusHistoryEntityType.INBOUND, saved.getInboundId(),
                    from.name(), saved.getStatus().name(), null, command.userId());
        }
        return saved;
    }

    /** DB를 보지 않고 요청 자체로 판단할 수 있는 규칙. */
    private void validateRequest(InboundInspectCommand command) {
        if (command.userId() == null) {
            throw validation("검수 처리자는 필수입니다.");
        }
        if (command.lines() == null || command.lines().isEmpty()) {
            throw validation("검수 항목을 1개 이상 입력해주세요.");
        }
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < command.lines().size(); i++) {
            InboundInspectCommand.Line line = command.lines().get(i);
            String at = "lines[" + i + "]";
            if (line.purchaseOrderLineId() == null) {
                throw validation(at + ".purchaseOrderLineId는 필수입니다.");
            }
            if (line.lotNumber() == null || line.lotNumber().isBlank()) {
                throw validation(at + ".lotNumber는 필수입니다.");
            }
            if (line.lotNumber().length() > LOT_NUMBER_MAX_LENGTH) {
                throw validation(at + ".lotNumber는 " + LOT_NUMBER_MAX_LENGTH + "자 이하여야 합니다.");
            }
            if (line.manufacturedDate() != null && line.expiryDate() != null
                    && line.expiryDate().isBefore(line.manufacturedDate())) {
                throw validation(at + ".expiryDate는 제조일 이후여야 합니다.");
            }
            if (line.receivedQuantity() <= 0) {
                throw validation(at + ".receivedQuantity는 0보다 커야 합니다.");
            }
            if (line.acceptedQuantity() < 0) {
                throw validation(at + ".acceptedQuantity는 0 이상이어야 합니다.");
            }
            if (line.defectiveQuantity() < 0) {
                throw validation(at + ".defectiveQuantity는 0 이상이어야 합니다.");
            }
            if (line.acceptedQuantity() + line.defectiveQuantity() != line.receivedQuantity()) {
                throw validation(at + ".defectiveQuantity: 합격 수량과 불량 수량의 합이 입고 수량과 같아야 합니다.");
            }
            if (line.receivedUnitPrice() == null || line.receivedUnitPrice().signum() < 0) {
                throw validation(at + ".receivedUnitPrice는 0 이상이어야 합니다.");
            }
            if (line.receivedUnitPrice().stripTrailingZeros().scale() > PRICE_MAX_SCALE) {
                throw validation(at + ".receivedUnitPrice는 소수 " + PRICE_MAX_SCALE + "자리까지 입력할 수 있습니다.");
            }
            if (line.priceChangeReason() != null && line.priceChangeReason().length() > PRICE_CHANGE_REASON_MAX_LENGTH) {
                throw validation(at + ".priceChangeReason은 " + PRICE_CHANGE_REASON_MAX_LENGTH + "자 이하여야 합니다.");
            }
            if (line.inspectionNote() != null && line.inspectionNote().length() > INSPECTION_NOTE_MAX_LENGTH) {
                throw validation(at + ".inspectionNote는 " + INSPECTION_NOTE_MAX_LENGTH + "자 이하여야 합니다.");
            }
            if (!keys.add(line.purchaseOrderLineId() + "/" + line.lotNumber())) {
                throw validation(at + ": 같은 발주 항목과 로트 번호 조합을 중복해서 입력할 수 없습니다.");
            }
        }
    }

    /** 요청의 발주 항목을 조회한다. 없으면 404, 다른 발주의 항목이면 400. */
    private Map<Long, PurchaseOrderLine> resolvePurchaseOrderLines(
            Inbound inbound, List<InboundInspectCommand.Line> lines) {
        Map<Long, PurchaseOrderLine> result = new LinkedHashMap<>();
        for (InboundInspectCommand.Line line : lines) {
            Long id = line.purchaseOrderLineId();
            if (result.containsKey(id)) {
                continue;
            }
            PurchaseOrderLine purchaseOrderLine = purchaseOrderRepository.findLineById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                            "발주 항목을 찾을 수 없습니다. purchaseOrderLineId=" + id));
            if (!purchaseOrderLine.getPurchaseOrderId().equals(inbound.getPurchaseOrderId())) {
                throw validation("이 입고의 발주에 속하지 않은 발주 항목입니다. purchaseOrderLineId=" + id);
            }
            result.put(id, purchaseOrderLine);
        }
        return result;
    }

    /** 발주 단가와 입고 단가가 다르면 변경 사유가 필수다. */
    private void validatePrices(Map<Long, PurchaseOrderLine> purchaseOrderLines,
                                List<InboundInspectCommand.Line> lines) {
        for (int i = 0; i < lines.size(); i++) {
            InboundInspectCommand.Line line = lines.get(i);
            BigDecimal ordered = purchaseOrderLines.get(line.purchaseOrderLineId()).getOrderedUnitPrice();
            boolean changed = ordered.compareTo(line.receivedUnitPrice()) != 0;
            boolean hasReason = line.priceChangeReason() != null && !line.priceChangeReason().isBlank();
            if (changed && !hasReason) {
                throw validation("lines[" + i + "].priceChangeReason: 발주 단가와 다른 입고 단가는 변경 사유가 필요합니다.");
            }
        }
    }

    /** 같은 발주 항목을 여러 로트로 나눠 받을 수 있으므로 발주 항목별 합계로 잔여 수량을 비교한다. */
    private void validateRemainingQuantities(Map<Long, PurchaseOrderLine> purchaseOrderLines,
                                             List<InboundInspectCommand.Line> lines) {
        Map<Long, Long> totals = new HashMap<>();
        for (InboundInspectCommand.Line line : lines) {
            totals.merge(line.purchaseOrderLineId(), line.receivedQuantity(), Long::sum);
        }
        totals.forEach((id, total) -> {
            long remaining = purchaseOrderLines.get(id).remainingQuantity();
            if (total > remaining) {
                throw validation("입고 수량이 발주 잔여 수량을 초과합니다. purchaseOrderLineId=" + id
                        + ", 잔여=" + remaining + ", 요청=" + total);
            }
        });
    }

    /** 구역은 입고 창고의 활성 구역이어야 하고, 합격 구역은 DEFECT가 아니며 불량 구역은 DEFECT여야 한다. */
    private void validateSections(Inbound inbound, List<InboundInspectCommand.Line> lines) {
        Map<Long, SectionInfo> sections = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            InboundInspectCommand.Line line = lines.get(i);
            if (line.acceptedSectionId() != null) {
                SectionInfo section = section(sections, line.acceptedSectionId());
                requireUsable(inbound, section, "lines[" + i + "].acceptedSectionId");
                if (section.isDefect()) {
                    throw validation("lines[" + i + "].acceptedSectionId: 합격품은 불량 구역에 둘 수 없습니다.");
                }
            }
            if (line.defectSectionId() != null) {
                SectionInfo section = section(sections, line.defectSectionId());
                requireUsable(inbound, section, "lines[" + i + "].defectSectionId");
                if (!section.isDefect()) {
                    throw validation("lines[" + i + "].defectSectionId: 불량품은 불량 구역(DEFECT)에 둬야 합니다.");
                }
            }
        }
    }

    private SectionInfo section(Map<Long, SectionInfo> cache, Long sectionId) {
        return cache.computeIfAbsent(sectionId, inboundSectionPort::getSection);
    }

    private void requireUsable(Inbound inbound, SectionInfo section, String field) {
        if (!section.warehouseId().equals(inbound.getWarehouseId())) {
            throw validation(field + ": 입고 창고에 속한 구역이 아닙니다.");
        }
        if (!section.active()) {
            throw validation(field + ": 비활성 구역은 지정할 수 없습니다.");
        }
    }

    private static BusinessException validation(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
    }
}

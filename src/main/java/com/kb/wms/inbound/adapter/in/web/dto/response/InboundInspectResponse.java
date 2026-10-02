package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.application.port.in.result.InboundLineView;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.enums.InboundStatus;

/**
 * PATCH /api/v1/inbounds/{inboundId}/inspect (입고 검수) 응답: 검수 후 입고 상태와 저장된 검수 항목.
 */
public record InboundInspectResponse(
        Long inboundId,
        String inboundNo,
        InboundStatus status,
        List<Item> items,
        LocalDateTime updatedAt
) {

    public record Item(
            Long inboundLineId,
            Long purchaseOrderLineId,
            Long skuId,
            String skuCode,
            Long lotId,
            String lotNumber,
            Long receivedQuantity,
            Long acceptedQuantity,
            Long defectiveQuantity,
            Long acceptedSectionId,
            Long defectSectionId,
            BigDecimal orderedUnitPrice,
            BigDecimal receivedUnitPrice,
            BigDecimal lineAmount
    ) {

        static Item from(InboundLineView view) {
            return new Item(
                    view.inboundLineId(), view.purchaseOrderLineId(), view.skuId(), view.skuCode(),
                    view.lotId(), view.lotNumber(), view.receivedQuantity(), view.acceptedQuantity(),
                    view.defectiveQuantity(), view.acceptedSectionId(), view.defectSectionId(),
                    view.orderedUnitPrice(), view.receivedUnitPrice(), view.lineAmount());
        }
    }

    public static InboundInspectResponse of(Inbound inbound, List<InboundLineView> lines) {
        return new InboundInspectResponse(
                inbound.getInboundId(), inbound.getInboundNo(), inbound.getStatus(),
                lines.stream().map(Item::from).toList(), inbound.getUpdatedAt());
    }
}

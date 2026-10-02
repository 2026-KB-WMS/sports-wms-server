package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.inbound.application.port.in.result.InboundLineView;

/**
 * GET /api/v1/inbounds/{inboundId}/details 응답의 검수 항목.
 */
public record InboundLineResponse(
        Long inboundLineId,
        Long purchaseOrderLineId,
        Long skuId,
        String skuCode,
        String skuName,
        Long lotId,
        String lotNumber,
        LocalDate manufacturedDate,
        LocalDate expiryDate,
        Long receivedQuantity,
        Long acceptedQuantity,
        Long defectiveQuantity,
        Long acceptedSectionId,
        String acceptedSectionCode,
        Long defectSectionId,
        String defectSectionCode,
        BigDecimal orderedUnitPrice,
        BigDecimal receivedUnitPrice,
        BigDecimal lineAmount,
        String priceChangeReason,
        String inspectionNote,
        LocalDateTime receivedAt,
        Long receivedBy
) {

    public static InboundLineResponse from(InboundLineView view) {
        return new InboundLineResponse(
                view.inboundLineId(), view.purchaseOrderLineId(), view.skuId(), view.skuCode(), view.skuName(),
                view.lotId(), view.lotNumber(), view.manufacturedDate(), view.expiryDate(),
                view.receivedQuantity(), view.acceptedQuantity(), view.defectiveQuantity(),
                view.acceptedSectionId(), view.acceptedSectionCode(), view.defectSectionId(),
                view.defectSectionCode(), view.orderedUnitPrice(), view.receivedUnitPrice(), view.lineAmount(),
                view.priceChangeReason(), view.inspectionNote(), view.receivedAt(), view.receivedBy());
    }
}

package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inbound.application.port.in.result.InboundSummary;
import com.kb.wms.inbound.domain.enums.InboundStatus;

/**
 * GET /api/v1/inbounds (입고 목록) 응답 항목.
 * 처리자 이름(receivedByName)은 검수 전이거나 취소된 입고에서 null이다.
 */
public record InboundSummaryResponse(
        Long inboundId,
        String inboundNo,
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        String warehouseName,
        InboundStatus status,
        LocalDateTime arrivedAt,
        LocalDateTime receivedAt,
        Long receivedBy,
        String receivedByName,
        Long lineCount
) {

    public static InboundSummaryResponse from(InboundSummary summary) {
        return new InboundSummaryResponse(
                summary.inboundId(), summary.inboundNo(), summary.purchaseOrderId(), summary.purchaseOrderNo(),
                summary.supplierId(), summary.supplierName(), summary.warehouseId(), summary.warehouseName(),
                summary.status(), summary.arrivedAt(), summary.receivedAt(), summary.receivedBy(),
                summary.receivedByName(), summary.lineCount());
    }
}

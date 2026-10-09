package com.kb.wms.inbound.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.InboundStatus;

/**
 * 입고 목록 항목 (GET /api/v1/inbounds). 검수 항목은 개수(lineCount)만 담으며 검수 전(ARRIVED)에는 0이다.
 * receivedBy의 사용자 이름은 명세의 목록 응답 항목에 없어서 담지 않는다(단건 InboundView에만 있다).
 */
public record InboundSummary(
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
        Long lineCount
) {
}

package com.kb.wms.outbound.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.outbound.domain.enums.OutboundStatus;

/**
 * 출고 목록 한 줄 (GET /api/v1/outbounds). 창고는 출고가 아니라 발주에 배정된 창고다.
 */
public record OutboundSummary(
        Long outboundId,
        String outboundNo,
        Long storeOrderId,
        String orderNo,
        Long storeId,
        String storeName,
        Long warehouseId,
        String warehouseName,
        OutboundStatus status,
        Long lineCount,
        LocalDateTime shippedAt,
        Long shippedBy,
        LocalDateTime createdAt
) {
}
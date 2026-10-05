package com.kb.wms.outbound.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.outbound.domain.enums.OutboundStatus;

/**
 * 출고 상세 헤더 (GET /api/v1/outbounds/{outboundId}/details). 항목은 {@link OutboundLineView}, 취소 사유는 상태 이력에서 읽는다.
 */
public record OutboundView(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        Long storeOrderId,
        String orderNo,
        Long storeId,
        String storeName,
        Long warehouseId,
        String warehouseName,
        LocalDateTime shippedAt,
        Long shippedBy,
        LocalDateTime deliveredAt,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
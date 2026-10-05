package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.outbound.application.port.in.result.OutboundSummary;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

/** GET /api/v1/outbounds 의 data.items[]. */
public record OutboundListItemResponse(
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

    public static OutboundListItemResponse from(OutboundSummary s) {
        return new OutboundListItemResponse(s.outboundId(), s.outboundNo(), s.storeOrderId(), s.orderNo(),
                s.storeId(), s.storeName(), s.warehouseId(), s.warehouseName(), s.status(), s.lineCount(),
                s.shippedAt(), s.shippedBy(), s.createdAt());
    }
}

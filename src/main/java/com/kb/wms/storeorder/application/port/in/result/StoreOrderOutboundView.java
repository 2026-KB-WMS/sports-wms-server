package com.kb.wms.storeorder.application.port.in.result;

import java.time.LocalDateTime;


/**
 * 지점 발주에 딸린 출고 한 건 (GET /api/v1/orders/{orderId}/details 의 outbounds[]).
 */
public record StoreOrderOutboundView(
        Long outboundId,
        String outboundNo,
        String status,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt
) {
}

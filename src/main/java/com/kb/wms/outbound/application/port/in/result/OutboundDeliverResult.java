package com.kb.wms.outbound.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 배송 완료 결과(PATCH /api/v1/outbounds/{outboundId}/deliver).
 *
 * @param storeOrderStatus 배송 완료 반영 뒤 발주 상태(전량 출고면 COMPLETED, 아니면 ASSIGNED)
 */
public record OutboundDeliverResult(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        LocalDateTime deliveredAt,
        Long storeOrderId,
        StoreOrderStatus storeOrderStatus,
        LocalDateTime updatedAt
) {
}

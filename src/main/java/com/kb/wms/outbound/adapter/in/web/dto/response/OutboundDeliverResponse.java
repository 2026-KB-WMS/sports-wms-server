package com.kb.wms.outbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.outbound.application.port.in.result.OutboundDeliverResult;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/** PATCH /api/v1/outbounds/{outboundId}/deliver 응답. */
public record OutboundDeliverResponse(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        LocalDateTime deliveredAt,
        StoreOrder storeOrder,
        LocalDateTime updatedAt
) {

    public record StoreOrder(Long storeOrderId, StoreOrderStatus status) {
    }

    public static OutboundDeliverResponse from(OutboundDeliverResult r) {
        return new OutboundDeliverResponse(r.outboundId(), r.outboundNo(), r.status(), r.deliveredAt(),
                new StoreOrder(r.storeOrderId(), r.storeOrderStatus()), r.updatedAt());
    }
}

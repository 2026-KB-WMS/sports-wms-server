package com.kb.wms.inbound.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * PATCH /api/v1/inbounds/{inboundId}/cancel (입고 취소) 응답. 취소해도 발주 상태는 바뀌지 않아 함께 내려준다.
 */
public record InboundCancelResponse(
        Long inboundId,
        String inboundNo,
        InboundStatus status,
        PurchaseOrder purchaseOrder,
        String cancelReason,
        LocalDateTime updatedAt
) {

    public record PurchaseOrder(Long purchaseOrderId, PurchaseOrderStatus status) {
    }

    public static InboundCancelResponse of(Inbound inbound, InboundView view) {
        return new InboundCancelResponse(
                inbound.getInboundId(), inbound.getInboundNo(), inbound.getStatus(),
                new PurchaseOrder(view.purchaseOrderId(), view.purchaseOrderStatus()), view.cancelReason(),
                inbound.getUpdatedAt());
    }
}

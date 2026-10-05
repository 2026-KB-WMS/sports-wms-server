package com.kb.wms.inbound.adapter.in.web.dto.request;

import com.kb.wms.inbound.application.port.in.command.PurchaseOrderCancelCommand;

import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/purchase-orders/{purchaseOrderId}/cancel 요청 바디.
 * 확정(CONFIRMED) 발주를 취소할 때는 사유가 필수이고, 요청(REQUESTED) 발주 취소 때는 선택이다.
 * 상태에 따른 필수 여부는 서비스가 검증한다.
 */
public record PurchaseOrderCancelRequest(
        @Size(max = 500, message = "취소 사유는 최대 500자입니다.")
        String reason
) {

    public PurchaseOrderCancelCommand toCommand(Long userId) {
        return new PurchaseOrderCancelCommand(reason, userId);
    }
}

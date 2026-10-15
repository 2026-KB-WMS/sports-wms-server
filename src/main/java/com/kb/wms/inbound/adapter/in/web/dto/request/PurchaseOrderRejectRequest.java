package com.kb.wms.inbound.adapter.in.web.dto.request;

import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRejectCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/purchase-orders/{purchaseOrderId}/reject 요청 바디. 반려 사유는 필수다.
 */
public record PurchaseOrderRejectRequest(
        @NotBlank(message = "반려 사유는 필수 값입니다.")
        @Size(max = 500, message = "반려 사유는 최대 500자입니다.")
        String reason
) {

    public PurchaseOrderRejectCommand toCommand(Long userId) {
        return new PurchaseOrderRejectCommand(reason, userId);
    }
}

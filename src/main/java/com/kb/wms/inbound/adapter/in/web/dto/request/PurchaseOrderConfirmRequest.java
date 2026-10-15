package com.kb.wms.inbound.adapter.in.web.dto.request;

import java.util.List;

import com.kb.wms.inbound.application.port.in.command.PurchaseOrderConfirmCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * PATCH /api/v1/purchase-orders/{purchaseOrderId}/confirm 요청 바디(선택).
 * 라인별 공급처를 지정한다. 지정하지 않은 라인은 이미 배정된 공급처를 유지하고,
 * 확정 후에는 모든 라인에 공급처가 있어야 한다.
 */
public record PurchaseOrderConfirmRequest(
        @Valid
        List<Line> lines
) {

    public record Line(
            @NotNull(message = "발주 항목 ID는 필수 값입니다.")
            Long purchaseOrderLineId,

            @NotNull(message = "공급처 ID는 필수 값입니다.")
            Long supplierId
    ) {
    }

    public PurchaseOrderConfirmCommand toCommand(Long userId) {
        List<PurchaseOrderConfirmCommand.Line> commandLines = lines == null ? List.of() : lines.stream()
                .map(line -> new PurchaseOrderConfirmCommand.Line(line.purchaseOrderLineId(), line.supplierId()))
                .toList();
        return new PurchaseOrderConfirmCommand(userId, commandLines);
    }
}

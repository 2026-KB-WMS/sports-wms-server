package com.kb.wms.inventory.adapter.in.web.dto.request;

import com.kb.wms.inventory.application.port.in.command.InventoryAdjustCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/inventory/adjustments 요청 바디.
 * 처리자(userId)는 인증이 도입되기 전까지 쿼리 파라미터로 받는다(창고 도메인의 GET /warehouses/my와 동일한 임시 방식).
 */
public record InventoryAdjustRequest(
        @NotNull(message = "조정할 재고 ID는 필수 값입니다.")
        Long inventoryLotId,

        @NotNull(message = "현재 보유 수량(beforeQuantity)은 필수 값입니다.")
        Long beforeQuantity,

        @NotNull(message = "조정 후 수량은 필수 값입니다.")
        @PositiveOrZero(message = "조정 후 수량은 0 이상이어야 합니다.")
        Long afterQuantity,

        @NotBlank(message = "조정 사유는 필수 값입니다.")
        @Size(max = 500, message = "조정 사유는 최대 500자입니다.")
        String reason
) {

    public InventoryAdjustCommand toCommand(Long userId) {
        return new InventoryAdjustCommand(inventoryLotId, beforeQuantity, afterQuantity, reason, userId);
    }
}

package com.kb.wms.storeorder.adapter.in.web.dto.request;

import com.kb.wms.storeorder.application.port.in.command.StoreOrderAssignCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/orders/assign 요청 바디. 재배정(이미 ASSIGNED인 발주의 창고 변경)은 사유가 필수이며 서비스가 검증한다.
 */
public record StoreOrderAssignRequest(
        @NotNull(message = "발주 ID는 필수 값입니다.")
        Long storeOrderId,

        @NotNull(message = "창고 ID는 필수 값입니다.")
        Long warehouseId,

        @Size(max = 500, message = "사유는 최대 500자입니다.")
        String reason
) {

    /**
     * @param userId 처리 사용자. 토큰 사용자의 ID다.
     */
    public StoreOrderAssignCommand toCommand(Long userId) {
        return new StoreOrderAssignCommand(storeOrderId, warehouseId, reason, userId);
    }
}

package com.kb.wms.inventory.adapter.in.web.dto.request;

import com.kb.wms.inventory.application.port.in.command.LotStatusChangeCommand;
import com.kb.wms.inventory.domain.enums.LotStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/lots/{lotId}/status 요청 바디.
 * 처리자는 토큰 사용자이며 요청 바디에 포함하지 않는다. EXPIRED는 서비스가 400으로 거절한다.
 */
public record LotStatusChangeRequest(
        @NotNull(message = "변경할 상태는 필수 값입니다.")
        LotStatus status,

        @NotBlank(message = "변경 사유는 필수 값입니다.")
        @Size(max = 500, message = "변경 사유는 최대 500자입니다.")
        String reason
) {

    public LotStatusChangeCommand toCommand(Long userId) {
        return new LotStatusChangeCommand(status, reason, userId);
    }
}

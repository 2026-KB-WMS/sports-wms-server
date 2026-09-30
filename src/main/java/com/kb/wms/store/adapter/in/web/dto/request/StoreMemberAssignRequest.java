package com.kb.wms.store.adapter.in.web.dto.request;

import com.kb.wms.store.application.port.in.command.StoreMemberAssignCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/v1/stores/assign 요청 바디.
 */
public record StoreMemberAssignRequest(
        @NotNull(message = "지점 ID는 필수 값입니다.")
        Long storeId,

        @NotNull(message = "사용자 ID는 필수 값입니다.")
        Long userId,

        @NotBlank(message = "담당 역할은 필수 값입니다.")
        String memberRole
) {

    public StoreMemberAssignCommand toCommand() {
        return new StoreMemberAssignCommand(storeId, userId, memberRole);
    }
}

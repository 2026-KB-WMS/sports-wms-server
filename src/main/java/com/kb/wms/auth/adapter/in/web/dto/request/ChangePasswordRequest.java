package com.kb.wms.auth.adapter.in.web.dto.request;

import com.kb.wms.auth.application.port.in.command.ChangePasswordCommand;

import jakarta.validation.constraints.NotBlank;

/**
 * PATCH /api/v1/auth/me/password 요청 바디. 새 비밀번호 형식은 서비스가 검사한다.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호는 필수 값입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수 값입니다.")
        String newPassword
) {

    public ChangePasswordCommand toCommand(Long userId) {
        return new ChangePasswordCommand(userId, currentPassword, newPassword);
    }
}

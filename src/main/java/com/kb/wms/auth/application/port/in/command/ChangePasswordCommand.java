package com.kb.wms.auth.application.port.in.command;

/**
 * PATCH /api/v1/auth/me/password 요청. userId는 토큰 주체 본인이다.
 */
public record ChangePasswordCommand(
        Long userId,
        String currentPassword,
        String newPassword
) {
}

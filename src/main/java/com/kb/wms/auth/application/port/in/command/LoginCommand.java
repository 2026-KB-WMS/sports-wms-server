package com.kb.wms.auth.application.port.in.command;

/**
 * POST /api/v1/auth/login 요청.
 */
public record LoginCommand(
        String loginId,
        String password
) {
}

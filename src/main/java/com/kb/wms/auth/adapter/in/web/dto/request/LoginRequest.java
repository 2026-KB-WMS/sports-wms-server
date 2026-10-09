package com.kb.wms.auth.adapter.in.web.dto.request;

import com.kb.wms.auth.application.port.in.command.LoginCommand;

import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/v1/auth/login 요청 바디.
 */
public record LoginRequest(
        @NotBlank(message = "로그인 아이디는 필수 값입니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수 값입니다.")
        String password
) {

    public LoginCommand toCommand() {
        return new LoginCommand(loginId, password);
    }
}

package com.kb.wms.auth.adapter.in.web.dto.request;

import com.kb.wms.auth.application.port.in.command.UserSignupCommand;
import com.kb.wms.auth.domain.enums.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/auth/signup 요청 바디. 아이디·비밀번호 형식 규칙은 서비스가 검사한다.
 */
public record SignupRequest(
        @NotBlank(message = "로그인 아이디는 필수 값입니다.")
        @Size(max = 50, message = "로그인 아이디는 최대 50자입니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수 값입니다.")
        String password,

        @NotBlank(message = "이름은 필수 값입니다.")
        @Size(max = 100, message = "이름은 최대 100자입니다.")
        String name,

        @NotBlank(message = "이메일은 필수 값입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 최대 255자입니다.")
        String email,

        @NotBlank(message = "연락처는 필수 값입니다.")
        @Size(max = 30, message = "연락처는 최대 30자입니다.")
        String phone,

        @NotNull(message = "역할은 필수 값입니다.")
        UserRole role
) {

    public UserSignupCommand toCommand() {
        return new UserSignupCommand(loginId, password, name, email, phone, role);
    }
}

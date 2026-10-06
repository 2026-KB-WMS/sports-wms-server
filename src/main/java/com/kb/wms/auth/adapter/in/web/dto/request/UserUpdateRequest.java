package com.kb.wms.auth.adapter.in.web.dto.request;

import com.kb.wms.auth.application.port.in.command.UserUpdateCommand;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/users/{userId} 요청 바디. 모든 필드는 선택이며, 값이 있는 필드만 수정한다.
 * loginId·password는 수정 대상이 아니지만 무시하지 않고 서비스가 400으로 거절하도록 그대로 넘긴다.
 */
public record UserUpdateRequest(
        @Pattern(regexp = NOT_BLANK, message = "이름은 공백일 수 없습니다.")
        @Size(max = 100, message = "이름은 최대 100자입니다.")
        String name,

        @Pattern(regexp = NOT_BLANK, message = "이메일은 공백일 수 없습니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 최대 255자입니다.")
        String email,

        @Pattern(regexp = NOT_BLANK, message = "연락처는 공백일 수 없습니다.")
        @Size(max = 30, message = "연락처는 최대 30자입니다.")
        String phone,

        UserRole role,

        UserStatus status,

        String loginId,

        String password
) {

    // 값을 보냈다면 공백 문자만으로 이루어질 수 없다. 생략(null)은 "수정 안 함"이라 통과한다.
    private static final String NOT_BLANK = ".*\\S.*";

    public UserUpdateCommand toCommand(Long userId, Long actorUserId) {
        return new UserUpdateCommand(userId, actorUserId, loginId, password, name, email, phone, role, status);
    }
}

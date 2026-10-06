package com.kb.wms.auth.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;

/**
 * POST /api/v1/auth/signup 응답.
 */
public record SignupResponse(
        Long userId,
        String loginId,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt
) {

    public static SignupResponse of(User user) {
        return new SignupResponse(user.getUserId(), user.getLoginId(), user.getName(), user.getEmail(),
                user.getPhone(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}

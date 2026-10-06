package com.kb.wms.auth.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;

/**
 * GET /api/v1/auth/me 응답.
 */
public record MyProfileResponse(
        Long userId,
        String loginId,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        LocalDateTime lastLoginAt
) {

    public static MyProfileResponse of(User user) {
        return new MyProfileResponse(user.getUserId(), user.getLoginId(), user.getName(), user.getEmail(),
                user.getPhone(), user.getRole(), user.getStatus(), user.getLastLoginAt());
    }
}

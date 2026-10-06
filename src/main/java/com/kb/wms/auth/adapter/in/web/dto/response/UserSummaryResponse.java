package com.kb.wms.auth.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;

/**
 * GET /api/v1/users 목록 응답 항목.
 */
public record UserSummaryResponse(
        Long userId,
        String loginId,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {

    public static UserSummaryResponse of(User user) {
        return new UserSummaryResponse(user.getUserId(), user.getLoginId(), user.getName(), user.getEmail(),
                user.getPhone(), user.getRole(), user.getStatus(), user.getLastLoginAt(), user.getCreatedAt());
    }
}

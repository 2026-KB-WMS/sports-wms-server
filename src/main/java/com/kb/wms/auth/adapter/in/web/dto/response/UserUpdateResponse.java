package com.kb.wms.auth.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;

/**
 * PATCH /api/v1/users/{userId} 응답.
 */
public record UserUpdateResponse(
        Long userId,
        String loginId,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        LocalDateTime updatedAt
) {

    public static UserUpdateResponse of(User user) {
        return new UserUpdateResponse(user.getUserId(), user.getLoginId(), user.getName(), user.getEmail(),
                user.getPhone(), user.getRole(), user.getStatus(), user.getUpdatedAt());
    }
}

package com.kb.wms.auth.adapter.in.web.dto.response;

import com.kb.wms.auth.application.port.in.result.LoginResult;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;

/**
 * POST /api/v1/auth/login 응답.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        LoginUser user
) {

    private static final String TOKEN_TYPE = "Bearer";

    public static LoginResponse of(LoginResult result) {
        return new LoginResponse(result.accessToken(), TOKEN_TYPE, result.expiresIn(), LoginUser.of(result.user()));
    }

    public record LoginUser(
            Long userId,
            String loginId,
            String name,
            UserRole role,
            UserStatus status
    ) {

        static LoginUser of(User user) {
            return new LoginUser(user.getUserId(), user.getLoginId(), user.getName(), user.getRole(), user.getStatus());
        }
    }
}

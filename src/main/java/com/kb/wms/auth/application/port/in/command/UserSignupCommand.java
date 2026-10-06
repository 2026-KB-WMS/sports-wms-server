package com.kb.wms.auth.application.port.in.command;

import com.kb.wms.auth.domain.enums.UserRole;

/**
 * POST /api/v1/auth/signup 요청. 창고 관리자·점주의 가입 신청이며 소속은 승인 과정에서 배정한다.
 */
public record UserSignupCommand(
        String loginId,
        String password,
        String name,
        String email,
        String phone,
        UserRole role
) {
}

package com.kb.wms.auth.application.port.in.query;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;

/**
 * GET /api/v1/users 검색 조건. null인 조건은 무시한다.
 *
 * @param role    역할 필터
 * @param status  계정 상태 필터 (가입 승인 대기 목록은 PENDING)
 * @param keyword 이름·로그인 아이디·이메일 부분 일치
 */
public record UserSearchCondition(
        UserRole role,
        UserStatus status,
        String keyword
) {
}

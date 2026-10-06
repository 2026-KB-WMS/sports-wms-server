package com.kb.wms.auth.domain.enums;

/**
 * 계정 상태 (User).
 *
 * <pre>
 * PENDING  → ACTIVE    승인
 * PENDING  → INACTIVE  가입 반려
 * ACTIVE   → INACTIVE  비활성화
 * INACTIVE → ACTIVE    재활성화
 * </pre>
 * PENDING으로 되돌리는 전이는 없다. 로그인은 ACTIVE 계정만 가능하다.
 */
public enum UserStatus {
    /** 가입 승인 대기 (가입 직후 기본값). */
    PENDING,
    /** 정상. */
    ACTIVE,
    /** 비활성 (가입 반려 또는 비활성화). */
    INACTIVE
}

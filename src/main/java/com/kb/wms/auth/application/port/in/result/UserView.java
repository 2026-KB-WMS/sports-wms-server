package com.kb.wms.auth.application.port.in.result;

/**
 * 다른 도메인이 사용자 배정 가능 여부를 확인할 때 쓰는 읽기용 값. 비밀번호 해시 등은 담지 않고,
 * 역할은 enum 이름 문자열, 상태는 활성(ACTIVE) 여부로만 내보낸다.
 */
public record UserView(
        Long userId,
        String name,
        String loginId,
        String role,
        boolean active
) {
}

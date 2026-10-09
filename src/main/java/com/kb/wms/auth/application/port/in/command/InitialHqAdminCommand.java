package com.kb.wms.auth.application.port.in.command;

/**
 * 시작 시 설정으로 받은 최초 본사 관리자 정보. 설정이 없는 값은 null 또는 빈 문자열이다.
 */
public record InitialHqAdminCommand(
        String loginId,
        String password,
        String name,
        String email,
        String phone
) {

    /** 다섯 값이 모두 비어 있으면 최초 관리자 생성을 설정하지 않은 것으로 본다. */
    public boolean isEmpty() {
        return isBlank(loginId) && isBlank(password) && isBlank(name) && isBlank(email) && isBlank(phone);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

package com.kb.wms.auth.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 인증·계정 도메인 특수 오류 코드.
 */
@Getter
public enum AuthErrorCode implements DomainErrorCode {

    USER_NOT_FOUND(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_LOGIN_ID(ErrorCode.CONFLICT, "이미 사용 중인 로그인 아이디입니다."),
    DUPLICATE_EMAIL(ErrorCode.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_USER_STATUS_TRANSITION(ErrorCode.CONFLICT, "허용되지 않는 계정 상태 변경입니다."),
    AFFILIATION_REQUIRED(ErrorCode.CONFLICT, "창고·지점 소속이 배정되지 않은 사용자는 활성화할 수 없습니다."),
    // 401이 아니라 400이다. 클라이언트가 401을 토큰 만료로 보고 로그아웃시키지 않도록 한다.
    CURRENT_PASSWORD_MISMATCH(ErrorCode.VALIDATION_ERROR, "현재 비밀번호가 올바르지 않습니다."),
    ACCOUNT_PENDING(ErrorCode.FORBIDDEN, "가입 승인 대기 중인 계정입니다. 본사 관리자의 승인 후 로그인할 수 있습니다."),
    ACCOUNT_INACTIVE(ErrorCode.FORBIDDEN, "비활성화된 계정입니다. 본사 관리자에게 문의해주세요."),
    AFFILIATION_ASSIGNED(ErrorCode.CONFLICT, "창고·지점 소속이 배정된 사용자는 역할을 변경할 수 없습니다. 소속을 먼저 회수해주세요.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    AuthErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}

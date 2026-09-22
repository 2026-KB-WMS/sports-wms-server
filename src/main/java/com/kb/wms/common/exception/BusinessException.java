package com.kb.wms.common.exception;

/**
 * 도메인 서비스 계층에서 던지는 커스텀 예외.
 * GlobalExceptionHandler가 ErrorCode를 기준으로 공통 오류 응답으로 매핑한다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String customErrorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(DomainErrorCode domainErrorCode) {
        this(domainErrorCode.getErrorCode(), domainErrorCode.getDefaultMessage(), domainErrorCode.name());
    }

    public BusinessException(DomainErrorCode domainErrorCode, String message) {
        this(domainErrorCode.getErrorCode(), message, domainErrorCode.name());
    }

    /**
     * 도메인 특수 오류 코드가 필요할 때 사용한다 (예: DUPLICATE_OPTION_VALUE).
     * 공통 API 규칙 문서 기준으로, HTTP 상태는 기본 ErrorCode를 따르되 error_code 문자열만 도메인 값으로 덮어쓴다.
     */
    public BusinessException(ErrorCode errorCode, String message, String customErrorCode) {
        super(message);
        this.errorCode = errorCode;
        this.customErrorCode = customErrorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorCodeName() {
        return customErrorCode != null ? customErrorCode : errorCode.name();
    }
}

package com.kb.wms.common.exception;

/**
 * 도메인 서비스 계층에서 던지는 커스텀 예외.
 * GlobalExceptionHandler가 ErrorCode를 기준으로 공통 오류 응답으로 매핑한다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

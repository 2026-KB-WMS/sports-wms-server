package com.kb.wms.common.response;

import com.kb.wms.common.exception.ErrorCode;

import java.util.List;

/**
 * 공통 오류 응답 포맷.
 * 공통 API 규칙 문서에 정의된 success/status_code/message/error_code/errors 스키마를 따른다.
 */
public record ErrorResponse(
        boolean success,
        int statusCode,
        String message,
        String errorCode,
        List<FieldError> errors
) {

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, errorCode.getStatus(), message, errorCode.name(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String errorCodeName, String message) {
        return new ErrorResponse(false, errorCode.getStatus(), message, errorCodeName, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldError> errors) {
        return new ErrorResponse(false, errorCode.getStatus(), message, errorCode.name(), errors);
    }

    public record FieldError(String field, String reason) {
    }
}

package com.kb.wms.common.response;

/**
 * 공통 성공 응답 포맷.
 * 공통 API 규칙 문서에 정의된 success/statusCode/message/data 스키마를 따른다.
 * JSON 필드명은 모두 camelCase다.
 */
public record ApiResponse<T>(
        boolean success,
        int statusCode,
        String message,
        T data
) {

    public static <T> ApiResponse<T> of(int statusCode, String message, T data) {
        return new ApiResponse<>(true, statusCode, message, data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return of(200, "요청에 성공하였습니다.", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return of(200, message, data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return of(201, "생성되었습니다.", data);
    }
}

package com.kb.wms.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 공통 성공 응답 포맷.
 * 공통 API 규칙 문서에 정의된 success/status_code/message/data 스키마를 따른다.
 * JSON 필드명은 명세대로 status_code(snake_case)를 쓰고, 나머지 데이터 필드는 그대로 camelCase를 유지한다.
 */
public record ApiResponse<T>(
        boolean success,
        @JsonProperty("status_code")
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

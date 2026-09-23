package com.kb.wms.common.response;

import java.util.List;

/**
 * 페이지네이션 없이 배열만 감싸는 목록 응답 포맷. 명세상 {@code data: { "items": [...] } } 형태를 쓰는
 * 고정 코드 목록(창고 관리 타입, 구역 유형 등)이나 소수 항목 목록(내 창고 조회 등)에 사용한다.
 */
public record ItemsResponse<T>(
        List<T> items
) {

    public static <T> ItemsResponse<T> of(List<T> items) {
        return new ItemsResponse<>(items);
    }
}

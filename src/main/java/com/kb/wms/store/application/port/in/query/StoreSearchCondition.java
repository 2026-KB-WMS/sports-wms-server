package com.kb.wms.store.application.port.in.query;

/**
 * GET /api/v1/stores 검색 조건. null인 조건은 무시한다.
 *
 * @param keyword  지점명·지점 코드 부분 일치
 * @param isActive 운영 상태 필터 (null이면 전체)
 */
public record StoreSearchCondition(
        String keyword,
        Boolean isActive
) {
}

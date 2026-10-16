package com.kb.wms.common.paging;

/**
 * 정렬 기준 하나.
 *
 * @param field     API에서 쓰는 정렬 필드 이름 (응답 {@code pageInfo.sort}에 그대로 나간다)
 * @param property  저장소 쿼리에서 쓰는 정렬 경로 (JPQL 별칭 경로, 예: {@code i.createdAt})
 * @param direction 정렬 방향
 */
public record SortOrder(String field, String property, SortDirection direction) {
}

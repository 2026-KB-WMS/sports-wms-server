package com.kb.wms.common.response;

import java.util.List;
import java.util.function.Function;

import com.kb.wms.common.paging.PageResult;

/**
 * 페이지네이션을 적용한 목록 응답 포맷: {@code data: { "items": [...], "pageInfo": {...} } }.
 */
public record PageResponse<T>(
        List<T> items,
        PageInfo pageInfo
) {

    public static <T> PageResponse<T> of(PageResult<T> result) {
        return new PageResponse<>(result.items(), PageInfo.of(result));
    }

    /** 결과 항목을 응답 DTO로 바꿔 담는다. */
    public static <S, T> PageResponse<T> of(PageResult<S> result, Function<? super S, ? extends T> mapper) {
        return of(result.map(mapper));
    }

    /**
     * @param page          1부터 시작하는 페이지 번호
     * @param size          요청한 페이지 크기
     * @param totalElements 조건에 맞는 전체 항목 수
     * @param totalPages    전체 페이지 수 (항목이 없으면 0)
     * @param sort          적용된 정렬 기준, {@code 필드,asc|desc} 형식 (요청에 sort가 없으면 기본 정렬)
     */
    public record PageInfo(int page, int size, long totalElements, int totalPages, String sort) {

        static PageInfo of(PageResult<?> result) {
            return new PageInfo(result.page(), result.size(), result.totalElements(), result.totalPages(), result.sort());
        }
    }
}

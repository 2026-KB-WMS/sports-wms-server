package com.kb.wms.common.paging;

import java.util.List;
import java.util.function.Function;

/**
 * 한 페이지 분량의 조회 결과. 저장소 어댑터가 만들어 UseCase 결과로 돌려준다.
 *
 * @param items         이 페이지의 항목
 * @param page          1부터 시작하는 페이지 번호
 * @param size          요청한 페이지 크기
 * @param totalElements 조건에 맞는 전체 항목 수
 * @param sort          적용된 첫 정렬 기준 ({@link PageCondition#sortLabel()})
 */
public record PageResult<T>(List<T> items, int page, int size, long totalElements, String sort) {

    public PageResult {
        items = List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, PageCondition condition, long totalElements) {
        return new PageResult<>(items, condition.page(), condition.size(), totalElements, condition.sortLabel());
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }

    /** 항목만 다른 타입으로 바꾼다. 페이지 정보는 그대로다. */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(items.stream().<R>map(mapper).toList(), page, size, totalElements, sort);
    }
}

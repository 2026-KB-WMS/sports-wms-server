package com.kb.wms.common.persistence;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.SortOrder;

/**
 * 포트의 {@link PageCondition}·{@link PageResult}와 Spring Data의 {@link Pageable}·{@link Page}를 오가는 변환.
 * 영속성 어댑터에서만 쓴다.
 */
public final class PageAdapter {

    private PageAdapter() {
    }

    /**
     * 정렬 경로({@link SortOrder#property()})를 그대로 JPQL 정렬에 넘긴다. 별칭 경로(예: {@code i.createdAt})를 쓴다.
     * 집계식 경로(예: {@code count(l)})는 Spring Data가 기본으로 거부하므로 {@code unsafe}로 넘긴다.
     * 경로는 컨트롤러의 {@link com.kb.wms.common.paging.SortSpec} 화이트리스트에서만 오므로 사용자 입력이 쿼리에 들어가지 않는다.
     */
    public static Pageable toPageable(PageCondition condition) {
        Sort sort = Sort.unsorted();
        for (SortOrder o : condition.orders()) {
            sort = sort.and(toSort(o));
        }
        return PageRequest.of(condition.page() - 1, condition.size(), sort);
    }

    private static Sort toSort(SortOrder o) {
        Sort.Direction direction = o.direction() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (o.property().contains("(")) {
            return JpaSort.unsafe(direction, o.property());
        }
        return Sort.by(direction, o.property());
    }

    public static <S, T> PageResult<T> toPageResult(Page<S> page, PageCondition condition, Function<? super S, ? extends T> mapper) {
        return PageResult.of(page.getContent().stream().<T>map(mapper).toList(), condition, page.getTotalElements());
    }

    public static <T> PageResult<T> toPageResult(Page<T> page, PageCondition condition) {
        return toPageResult(page, condition, Function.identity());
    }
}

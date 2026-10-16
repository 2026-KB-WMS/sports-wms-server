package com.kb.wms.common.persistence;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

    /** 정렬 경로({@link SortOrder#property()})를 그대로 JPQL 정렬에 넘긴다. 별칭 경로(예: {@code i.createdAt})를 쓴다. */
    public static Pageable toPageable(PageCondition condition) {
        List<Sort.Order> orders = condition.orders().stream()
                .map(o -> o.direction() == SortDirection.ASC ? Sort.Order.asc(o.property()) : Sort.Order.desc(o.property()))
                .toList();
        return PageRequest.of(condition.page() - 1, condition.size(), Sort.by(orders));
    }

    public static <S, T> PageResult<T> toPageResult(Page<S> page, PageCondition condition, Function<? super S, ? extends T> mapper) {
        return PageResult.of(page.getContent().stream().<T>map(mapper).toList(), condition, page.getTotalElements());
    }

    public static <T> PageResult<T> toPageResult(Page<T> page, PageCondition condition) {
        return toPageResult(page, condition, Function.identity());
    }
}

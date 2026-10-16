package com.kb.wms.common.paging;

import java.util.List;

/**
 * 페이지네이션을 적용한 목록 테스트에서 쓰는 페이지 조건·결과 도우미.
 */
public final class TestPages {

    private TestPages() {
    }

    /** 첫 페이지, 넉넉한 크기(100), 주어진 정렬. 정렬은 쿼리 경로 기준이며 마지막은 ID 보조 정렬이다. */
    public static PageCondition firstPage(SortOrder... orders) {
        return new PageCondition(1, 100, List.of(orders));
    }

    public static SortOrder order(String property, SortDirection direction) {
        return new SortOrder(property, property, direction);
    }

    /** 컨트롤러·서비스 테스트에서 목 유스케이스가 돌려줄 결과 (한 페이지에 전부 담김) */
    public static <T> PageResult<T> resultOf(List<T> items) {
        return new PageResult<>(items, 1, 20, items.size(), "createdAt,desc");
    }
}

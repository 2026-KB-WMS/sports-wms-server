package com.kb.wms.common.paging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kb.wms.common.response.PageResponse;

class PageResultTest {

    private static final SortSpec SPEC = SortSpec.builder()
            .field("createdAt", "i.createdAt")
            .defaultOrder("createdAt", SortDirection.DESC)
            .tieBreaker("i.id")
            .build();

    @Test
    @DisplayName("전체 페이지 수는 올림이고 항목이 없으면 0이다")
    void totalPages() {
        PageCondition condition = PageCondition.of(1, 20, null, SPEC);

        assertThat(PageResult.of(List.of(), condition, 0).totalPages()).isZero();
        assertThat(PageResult.of(List.of(1), condition, 1).totalPages()).isEqualTo(1);
        assertThat(PageResult.of(List.of(1), condition, 20).totalPages()).isEqualTo(1);
        assertThat(PageResult.of(List.of(1), condition, 21).totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("응답은 items와 pageInfo(page, size, totalElements, totalPages, sort)를 담는다")
    void response() {
        PageCondition condition = PageCondition.of(2, 2, "createdAt,asc", SPEC);
        PageResult<Integer> result = PageResult.of(List.of(3, 4), condition, 5);

        PageResponse<String> response = PageResponse.of(result, n -> "item-" + n);

        assertThat(response.items()).containsExactly("item-3", "item-4");
        assertThat(response.pageInfo().page()).isEqualTo(2);
        assertThat(response.pageInfo().size()).isEqualTo(2);
        assertThat(response.pageInfo().totalElements()).isEqualTo(5);
        assertThat(response.pageInfo().totalPages()).isEqualTo(3);
        assertThat(response.pageInfo().sort()).isEqualTo("createdAt,asc");
    }
}

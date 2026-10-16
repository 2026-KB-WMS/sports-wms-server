package com.kb.wms.common.paging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;

class PageConditionTest {

    private static final SortSpec SPEC = SortSpec.builder()
            .field("createdAt", "i.createdAt")
            .field("status", "i.status")
            .field("id", "i.inboundId")
            .defaultOrder("createdAt", SortDirection.DESC)
            .tieBreaker("i.inboundId")
            .build();

    @Test
    @DisplayName("아무것도 보내지 않으면 1페이지, 20건, 기본 정렬이다")
    void defaults() {
        PageCondition condition = PageCondition.of(null, null, null, SPEC);

        assertThat(condition.page()).isEqualTo(1);
        assertThat(condition.size()).isEqualTo(20);
        assertThat(condition.offset()).isZero();
        assertThat(condition.sortLabel()).isEqualTo("createdAt,desc");
        assertThat(condition.orders()).extracting(SortOrder::property).containsExactly("i.createdAt", "i.inboundId");
        assertThat(condition.orders()).extracting(SortOrder::direction)
                .containsExactly(SortDirection.DESC, SortDirection.DESC);
    }

    @Test
    @DisplayName("빈 sort는 기본 정렬로 본다")
    void blankSortIsDefault() {
        assertThat(PageCondition.of(1, 20, "  ", SPEC).sortLabel()).isEqualTo("createdAt,desc");
    }

    @Test
    @DisplayName("sort를 주면 그 정렬 뒤에 ID 보조 정렬이 첫 정렬과 같은 방향으로 붙는다")
    void sortWithTieBreaker() {
        PageCondition condition = PageCondition.of(3, 50, "status,asc", SPEC);

        assertThat(condition.offset()).isEqualTo(100);
        assertThat(condition.sortLabel()).isEqualTo("status,asc");
        assertThat(condition.orders()).extracting(SortOrder::property).containsExactly("i.status", "i.inboundId");
        assertThat(condition.orders()).extracting(SortOrder::direction)
                .containsExactly(SortDirection.ASC, SortDirection.ASC);
    }

    @Test
    @DisplayName("방향을 생략하면 asc이고 대소문자와 공백은 무시한다")
    void directionDefaultsAndCase() {
        assertThat(PageCondition.of(1, 20, "status", SPEC).sortLabel()).isEqualTo("status,asc");
        assertThat(PageCondition.of(1, 20, " status , DESC ", SPEC).sortLabel()).isEqualTo("status,desc");
    }

    @Test
    @DisplayName("ID 자체로 정렬하면 보조 정렬을 한 번만 붙인다")
    void sortByTieBreakerItself() {
        PageCondition condition = PageCondition.of(1, 20, "id,desc", SPEC);

        assertThat(condition.orders()).hasSize(1);
        assertThat(condition.orders().get(0).property()).isEqualTo("i.inboundId");
    }

    @Test
    @DisplayName("size는 100까지 허용한다")
    void maxSize() {
        assertThat(PageCondition.of(1, 100, null, SPEC).size()).isEqualTo(100);
    }

    @Test
    @DisplayName("범위를 벗어난 page·size, 허용되지 않은 sort는 400 VALIDATION_ERROR다")
    void invalidParameters() {
        assertValidationError(() -> PageCondition.of(0, 20, null, SPEC), "page");
        assertValidationError(() -> PageCondition.of(-1, 20, null, SPEC), "page");
        assertValidationError(() -> PageCondition.of(1, 0, null, SPEC), "size");
        assertValidationError(() -> PageCondition.of(1, 101, null, SPEC), "size");
        assertValidationError(() -> PageCondition.of(1, 20, "unknown,asc", SPEC), "createdAt, status, id");
        assertValidationError(() -> PageCondition.of(1, 20, "status,up", SPEC), "asc 또는 desc");
        assertValidationError(() -> PageCondition.of(1, 20, "status,asc,id", SPEC), "하나만");
        assertValidationError(() -> PageCondition.of(1, 20, ",asc", SPEC), "createdAt, status, id");
    }

    @Test
    @DisplayName("기본 정렬 필드가 허용 필드에 없거나 규칙이 비어 있으면 만들 수 없다")
    void invalidSpec() {
        assertThatThrownBy(() -> SortSpec.builder().field("a", "x.a").defaultOrder("b", SortDirection.ASC)
                .tieBreaker("x.id").build()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SortSpec.builder().field("a", "x.a").tieBreaker("x.id").build())
                .isInstanceOf(IllegalStateException.class);
    }

    private static void assertValidationError(Runnable call, String messagePart) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(e.getMessage()).contains(messagePart);
                });
    }
}

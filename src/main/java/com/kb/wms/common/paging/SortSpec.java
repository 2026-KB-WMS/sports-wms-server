package com.kb.wms.common.paging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 목록 API 하나의 정렬 규칙: 정렬할 수 있는 필드(화이트리스트), 기본 정렬, 마지막에 붙일 ID 보조 정렬.
 * 컨트롤러가 상수로 하나씩 정의하고 {@link PageCondition#of}에 넘긴다.
 */
public final class SortSpec {

    private final Map<String, String> fields;
    private final List<SortOrder> defaultOrders;
    private final String tieBreakerProperty;

    private SortSpec(Map<String, String> fields, List<SortOrder> defaultOrders, String tieBreakerProperty) {
        this.fields = fields;
        this.defaultOrders = defaultOrders;
        this.tieBreakerProperty = tieBreakerProperty;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** API 필드 이름 → 쿼리 정렬 경로 */
    Map<String, String> fields() {
        return fields;
    }

    List<SortOrder> defaultOrders() {
        return defaultOrders;
    }

    /** 정렬 목록 뒤에 ID 보조 정렬을 붙인다. 이미 같은 경로가 있으면 붙이지 않는다. 방향은 첫 정렬을 따른다. */
    List<SortOrder> withTieBreaker(List<SortOrder> orders) {
        List<SortOrder> result = new ArrayList<>(orders);
        boolean present = orders.stream().anyMatch(o -> o.property().equals(tieBreakerProperty));
        if (!present) {
            result.add(new SortOrder(tieBreakerProperty, tieBreakerProperty, orders.get(0).direction()));
        }
        return List.copyOf(result);
    }

    public static final class Builder {

        private record DefaultOrder(String field, SortDirection direction) {
        }

        private final Map<String, String> fields = new LinkedHashMap<>();
        private final List<DefaultOrder> defaults = new ArrayList<>();
        private String tieBreakerProperty;

        /** 정렬을 허용하는 필드. {@code field}는 API 이름, {@code property}는 쿼리의 정렬 경로다. */
        public Builder field(String field, String property) {
            fields.put(field, property);
            return this;
        }

        /** 기본 정렬. 여러 번 부르면 부른 순서대로 적용한다. 허용 필드여야 한다. */
        public Builder defaultOrder(String field, SortDirection direction) {
            defaults.add(new DefaultOrder(field, direction));
            return this;
        }

        /** 같은 값이 많아도 페이지 사이 순서가 고정되도록 마지막에 붙일 ID 경로. */
        public Builder tieBreaker(String property) {
            this.tieBreakerProperty = property;
            return this;
        }

        public SortSpec build() {
            if (fields.isEmpty() || defaults.isEmpty() || tieBreakerProperty == null) {
                throw new IllegalStateException("정렬 규칙에는 허용 필드, 기본 정렬, ID 보조 정렬이 모두 필요합니다.");
            }
            List<SortOrder> orders = new ArrayList<>();
            for (DefaultOrder d : defaults) {
                String property = fields.get(d.field());
                if (property == null) {
                    throw new IllegalStateException("기본 정렬 필드가 허용 필드에 없습니다: " + d.field());
                }
                orders.add(new SortOrder(d.field(), property, d.direction()));
            }
            return new SortSpec(Collections.unmodifiableMap(new LinkedHashMap<>(fields)), List.copyOf(orders),
                    tieBreakerProperty);
        }
    }
}

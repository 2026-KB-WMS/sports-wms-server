package com.kb.wms.common.paging;

import java.util.Locale;
import java.util.Optional;

public enum SortDirection {
    ASC, DESC;

    /** {@code asc}·{@code desc}를 대소문자 구분 없이 읽는다. 그 밖의 값은 빈 값이다. */
    static Optional<SortDirection> parse(String value) {
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String lowerName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

package com.kb.wms.common.persistence;

import org.springframework.util.StringUtils;

/**
 * 목록 조회의 keyword(부분 일치 검색) 파라미터 정규화.
 */
public final class SearchKeyword {

    private SearchKeyword() {
    }

    /** 빈 문자열·공백만 있는 검색어는 조건 없음(null)으로 보고, 나머지는 앞뒤 공백을 제거한다. */
    public static String normalize(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}

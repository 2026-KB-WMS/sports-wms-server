package com.kb.wms.inventory.application.port.in.query;

import java.time.LocalDate;

/**
 * GET /api/v1/lots 검색 조건.
 *
 * @param expiringBefore 이 날짜(당일 포함) 이전에 만료되는 로트만
 * @param keyword        로트 번호 부분 일치
 */
public record LotSearchCondition(
        Long skuId,
        Long supplierId,
        LocalDate expiringBefore,
        String keyword
) {
}

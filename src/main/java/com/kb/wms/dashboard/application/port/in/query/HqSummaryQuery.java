package com.kb.wms.dashboard.application.port.in.query;

import java.time.YearMonth;

/**
 * GET /api/v1/dashboard/hq/summary 조회 조건. null이면 서비스가 기본값을 채운다.
 *
 * @param month              집계 기준 월 (기본 현재 달)
 * @param expiringWithinDays 유통기한 임박 기준 일수, 1~365 (기본 30)
 */
public record HqSummaryQuery(
        YearMonth month,
        Integer expiringWithinDays
) {
}

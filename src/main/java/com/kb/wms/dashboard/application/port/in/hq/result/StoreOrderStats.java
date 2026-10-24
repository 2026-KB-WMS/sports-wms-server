package com.kb.wms.dashboard.application.port.in.hq.result;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 기준 월에 요청된 지점 발주의 상태 분포.
 *
 * @param byStatus       상태 이름 → 건수. 모든 상태를 0건이어도 포함한다
 * @param completionRate COMPLETED / (totalCount - CANCELED - REJECTED), 소수 둘째 자리. 분모가 0이면 null
 */
public record StoreOrderStats(
        Long totalCount,
        Map<String, Long> byStatus,
        BigDecimal completionRate
) {
}

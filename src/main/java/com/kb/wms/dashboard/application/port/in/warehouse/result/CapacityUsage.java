package com.kb.wms.dashboard.application.port.in.warehouse.result;

import java.math.BigDecimal;

/**
 * 창고의 수용량 사용 현황. 이름과 의미는 구역 API와 같다.
 *
 * @param capacity          활성 최상위 구역의 수용량 합 (하위 구역 수용량은 상위 구역에 포함되므로 더하지 않는다)
 * @param currentCapacity   활성 구역 전체의 적치량 합 (적치는 구역 단위로만 늘고 상위로 합산되지 않는다)
 * @param availableCapacity capacity - currentCapacity
 * @param usageRate         currentCapacity / capacity, 소수 둘째 자리. capacity가 0이면 null
 */
public record CapacityUsage(
        BigDecimal capacity,
        BigDecimal currentCapacity,
        BigDecimal availableCapacity,
        BigDecimal usageRate
) {
}

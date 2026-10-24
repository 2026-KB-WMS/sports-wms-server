package com.kb.wms.dashboard.application.port.in.result;

import java.time.YearMonth;

/**
 * 월별 입출고 완료 수량.
 *
 * @param inboundInProgressQuantity  현재 달에만 값이 있고 지난 달은 null(진행중은 현재 시점 스냅샷이므로)
 * @param outboundInProgressQuantity 현재 달에만 값이 있고 지난 달은 null
 */
public record MonthlyFlow(
        YearMonth month,
        Long inboundCompletedQuantity,
        Long outboundCompletedQuantity,
        Long inboundInProgressQuantity,
        Long outboundInProgressQuantity
) {
}

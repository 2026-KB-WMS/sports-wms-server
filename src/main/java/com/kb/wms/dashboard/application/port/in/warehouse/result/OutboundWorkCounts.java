package com.kb.wms.dashboard.application.port.in.warehouse.result;

/**
 * 담당 창고의 출고 작업 대기 건수. 현재 시점 전체를 센다.
 *
 * @param readyCount   피킹 전(READY)
 * @param pickingCount 작업 중(PICKING, PICKED)
 * @param shippedCount 배송 중(SHIPPED)
 */
public record OutboundWorkCounts(
        Long readyCount,
        Long pickingCount,
        Long shippedCount
) {
}

package com.kb.wms.dashboard.application.port.in.warehouse.result;

/**
 * 담당 창고의 입고 작업 대기 건수. 현재 시점 전체를 센다.
 *
 * @param arrivedCount    검수 전(ARRIVED)
 * @param inspectingCount 검수 중(INSPECTING)
 */
public record InboundWorkCounts(
        Long arrivedCount,
        Long inspectingCount
) {
}

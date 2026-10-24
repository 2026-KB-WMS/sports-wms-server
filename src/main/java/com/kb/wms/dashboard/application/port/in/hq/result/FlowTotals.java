package com.kb.wms.dashboard.application.port.in.hq.result;

/**
 * 입고 또는 출고의 진행중/완료 집계. 시작 전(입고 ARRIVED, 출고 READY)과 취소는 세지 않는다.
 *
 * @param completed              기준 월에 완료된 건
 * @param inProgress             월과 무관한 현재 시점의 진행중 건
 * @param previousMonthCompleted 기준 월 전월에 완료된 건(증감 표시용)
 */
public record FlowTotals(
        FlowCount completed,
        FlowCount inProgress,
        FlowCount previousMonthCompleted
) {
}

package com.kb.wms.dashboard.application.port.in.warehouse.result;

/**
 * 담당 창고가 지금 처리해야 할 입고·출고 작업 건수. 취소·완료 건은 세지 않는다.
 */
public record WarehouseWorkQueue(
        InboundWorkCounts inbound,
        OutboundWorkCounts outbound
) {
}

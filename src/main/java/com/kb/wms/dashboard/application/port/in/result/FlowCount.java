package com.kb.wms.dashboard.application.port.in.result;

/**
 * 입고 또는 출고의 건수와 수량 합.
 */
public record FlowCount(
        Long count,
        Long quantity
) {
}

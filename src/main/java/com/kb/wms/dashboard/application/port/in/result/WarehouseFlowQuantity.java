package com.kb.wms.dashboard.application.port.in.result;

/**
 * 창고 하나의 입고 또는 출고 수량(진행중/완료).
 */
public record WarehouseFlowQuantity(
        Long completedQuantity,
        Long inProgressQuantity
) {
}

package com.kb.wms.storeorder.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 창고 배정·재배정 처리 결과 (POST /api/v1/orders/assign 응답).
 */
public record StoreOrderAssignResult(
        Long storeOrderId,
        String orderNo,
        StoreOrderStatus status,
        Long warehouseId,
        String warehouseName,
        LocalDateTime updatedAt
) {
}

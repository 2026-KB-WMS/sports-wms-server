package com.kb.wms.storeorder.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderAssignResult;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * POST /api/v1/orders/assign 응답.
 */
public record StoreOrderAssignResponse(
        Long storeOrderId,
        String orderNo,
        StoreOrderStatus status,
        Long warehouseId,
        String warehouseName,
        LocalDateTime updatedAt
) {

    public static StoreOrderAssignResponse from(StoreOrderAssignResult result) {
        return new StoreOrderAssignResponse(
                result.storeOrderId(), result.orderNo(), result.status(),
                result.warehouseId(), result.warehouseName(), result.updatedAt());
    }
}

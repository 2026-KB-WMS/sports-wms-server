package com.kb.wms.storeorder.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderListItem;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderSummary;
import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderProgressStage;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * GET /api/v1/orders (발주 목록) 응답 항목.
 */
public record StoreOrderListItemResponse(
        Long storeOrderId,
        String orderNo,
        Long storeId,
        String storeName,
        Long warehouseId,
        String warehouseName,
        StoreOrderStatus status,
        StoreOrderProgressStage progressStage,
        LocalDateTime requestedAt,
        LocalDateTime requestedDeliveryAt,
        Long lineCount,
        BigDecimal totalAmount,
        StoreOrderOutboundStatus latestOutboundStatus
) {

    public static StoreOrderListItemResponse from(StoreOrderListItem item) {
        StoreOrderSummary summary = item.summary();
        return new StoreOrderListItemResponse(
                summary.storeOrderId(),
                summary.orderNo(),
                summary.storeId(),
                summary.storeName(),
                summary.warehouseId(),
                summary.warehouseName(),
                summary.status(),
                item.progressStage(),
                summary.requestedAt(),
                summary.requestedDeliveryAt(),
                summary.lineCount(),
                summary.totalAmount(),
                item.latestOutboundStatus());
    }
}

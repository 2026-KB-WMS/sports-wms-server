package com.kb.wms.storeorder.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderLineView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderView;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * POST /api/v1/orders 응답. 방금 등록한 발주 헤더와 항목을 함께 담는다.
 */
public record StoreOrderRegisterResponse(
        Long storeOrderId,
        String orderNo,
        Long storeId,
        String storeName,
        Long warehouseId,
        StoreOrderStatus status,
        LocalDateTime requestedAt,
        LocalDateTime requestedDeliveryAt,
        String note,
        BigDecimal totalAmount,
        Long createdBy,
        List<StoreOrderLineResponse> lines,
        LocalDateTime createdAt
) {

    public static StoreOrderRegisterResponse of(StoreOrderView view, List<StoreOrderLineView> lines) {
        return new StoreOrderRegisterResponse(
                view.storeOrderId(),
                view.orderNo(),
                view.storeId(),
                view.storeName(),
                view.warehouseId(),
                view.status(),
                view.requestedAt(),
                view.requestedDeliveryAt(),
                view.note(),
                view.totalAmount(),
                view.createdBy(),
                lines.stream().map(StoreOrderLineResponse::from).toList(),
                view.createdAt());
    }
}

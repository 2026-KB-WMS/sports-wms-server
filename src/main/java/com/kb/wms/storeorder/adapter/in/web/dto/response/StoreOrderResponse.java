package com.kb.wms.storeorder.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderDetail;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderView;
import com.kb.wms.storeorder.domain.enums.StoreOrderProgressStage;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * GET /api/v1/orders/{orderId} (발주 헤더 단건) 응답.
 * 작성자 이름(createdByName)은 회원 도메인이 없어 null로 내려주고, 회원 도메인 연동 후 채운다.
 */
public record StoreOrderResponse(
        Long storeOrderId,
        String orderNo,
        Long storeId,
        String storeName,
        Long warehouseId,
        String warehouseName,
        StoreOrderStatus status,
        String statusReason,
        StoreOrderProgressStage progressStage,
        LocalDateTime requestedAt,
        LocalDateTime requestedDeliveryAt,
        String note,
        Long lineCount,
        BigDecimal totalAmount,
        Long createdBy,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static StoreOrderResponse from(StoreOrderDetail detail) {
        StoreOrderView view = detail.view();
        return new StoreOrderResponse(
                view.storeOrderId(),
                view.orderNo(),
                view.storeId(),
                view.storeName(),
                view.warehouseId(),
                view.warehouseName(),
                view.status(),
                detail.statusReason(),
                detail.progressStage(),
                view.requestedAt(),
                view.requestedDeliveryAt(),
                view.note(),
                view.lineCount(),
                view.totalAmount(),
                view.createdBy(),
                null,
                view.createdAt(),
                view.updatedAt());
    }
}

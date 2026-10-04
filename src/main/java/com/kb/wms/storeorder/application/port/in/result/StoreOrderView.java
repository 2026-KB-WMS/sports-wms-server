package com.kb.wms.storeorder.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 지점 발주 헤더 단건 (GET /api/v1/orders/{orderId}).
 * 사유(statusReason)·진행 단계·작성자 이름은 서비스가 StatusHistory·출고 연동 포트·회원 도메인으로 채운다.
 *
 * @param shortageLineCount 출고 수량이 요청 수량에 못 미치는 항목 수
 */
public record StoreOrderView(
        Long storeOrderId,
        String orderNo,
        Long storeId,
        String storeName,
        Long warehouseId,
        String warehouseName,
        StoreOrderStatus status,
        LocalDateTime requestedAt,
        LocalDateTime requestedDeliveryAt,
        String note,
        Long lineCount,
        BigDecimal totalAmount,
        Long shortageLineCount,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public boolean hasShortage() {
        return shortageLineCount != null && shortageLineCount > 0;
    }
}

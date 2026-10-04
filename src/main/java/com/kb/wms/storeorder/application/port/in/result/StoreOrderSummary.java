package com.kb.wms.storeorder.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 지점 발주 목록 한 행 (GET /api/v1/orders). 진행 단계·최근 출고 상태는 서비스가 출고 연동 포트와 함께 채운다.
 *
 * @param warehouseId        배정 전(REQUESTED·APPROVED, 반려·취소된 미배정 발주)에는 null
 * @param warehouseName      warehouseId가 null이면 null
 * @param lineCount          발주 항목 수
 * @param totalAmount        항목 금액(요청 수량 × 공급 단가) 합계
 * @param shortageLineCount  출고 수량이 요청 수량에 못 미치는 항목 수
 */
public record StoreOrderSummary(
        Long storeOrderId,
        String orderNo,
        Long storeId,
        String storeName,
        Long warehouseId,
        String warehouseName,
        StoreOrderStatus status,
        LocalDateTime requestedAt,
        LocalDateTime requestedDeliveryAt,
        Long lineCount,
        BigDecimal totalAmount,
        Long shortageLineCount
) {

    public boolean hasShortage() {
        return shortageLineCount != null && shortageLineCount > 0;
    }
}

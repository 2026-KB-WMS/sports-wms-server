package com.kb.wms.storeorder.application.port.in.command;

/**
 * POST /api/v1/orders/assign 요청. APPROVED 발주는 최초 배정, ASSIGNED 발주는 재배정이다.
 *
 * @param storeOrderId 배정할 발주
 * @param warehouseId  배정할 창고 (활성 창고)
 * @param reason       사유 (500자 이하). 재배정(이미 ASSIGNED인 발주의 창고 변경)에서는 필수
 * @param changedBy    처리 사용자 (토큰 사용자의 ID)
 */
public record StoreOrderAssignCommand(
        Long storeOrderId,
        Long warehouseId,
        String reason,
        Long changedBy
) {
}

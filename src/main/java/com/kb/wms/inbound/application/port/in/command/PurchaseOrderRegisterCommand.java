package com.kb.wms.inbound.application.port.in.command;

import java.time.LocalDateTime;
import java.util.List;

/**
 * POST /api/v1/purchase-orders 요청. 발주 단가는 요청에서 받지 않고 등록 시점의 SKU 매입 단가를 스냅샷한다.
 *
 * @param expectedAt 입고 예정 일시 (선택)
 * @param note       발주 비고 (선택)
 * @param createdBy  요청 사용자 (토큰의 사용자)
 */
public record PurchaseOrderRegisterCommand(
        Long warehouseId,
        Long supplierId,
        LocalDateTime expectedAt,
        String note,
        Long createdBy,
        List<Line> lines
) {

    /**
     * @param expectedQuantity 발주(요청) 수량
     */
    public record Line(Long skuId, long expectedQuantity) {
    }
}

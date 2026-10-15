package com.kb.wms.inbound.application.port.in.command;

import java.util.List;

/**
 * PATCH /api/v1/purchase-orders/{purchaseOrderId}/confirm 요청.
 * 라인에 공급처를 지정하면서 확정한다. 지정하지 않은 라인은 이미 배정된 공급처를 유지하며,
 * 확정 후에는 모든 라인에 공급처가 있어야 한다.
 *
 * @param userId 확정 처리 사용자 (토큰 사용자의 ID). 상태 이력의 처리자로 기록한다
 * @param lines  공급처를 지정할 라인 (선택, 빈 목록이면 지정 없음)
 */
public record PurchaseOrderConfirmCommand(
        Long userId,
        List<Line> lines
) {

    public PurchaseOrderConfirmCommand {
        lines = lines == null ? List.of() : lines;
    }

    /**
     * @param purchaseOrderLineId 이 발주의 항목 ID
     * @param supplierId          항목의 SKU에 매핑된 활성 공급처
     */
    public record Line(Long purchaseOrderLineId, Long supplierId) {
    }
}

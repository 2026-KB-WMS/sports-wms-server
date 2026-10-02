package com.kb.wms.inbound.application.port.in.result;

import java.util.List;

import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * 입고 완료 처리 결과 (PATCH /api/v1/inbounds/{inboundId}/complete): 완료된 입고와 이번 완료로 반영된
 * 재고 행, 갱신된 발주·발주 항목 상태.
 *
 * @param inventory          검수 항목별로 반영된 재고 행. 합격·불량 수량이 없는 쪽의 inventoryLotId는 null
 * @param purchaseOrderLines 이 입고가 건드린 발주 항목의 누적 입고 수량·상태
 */
public record InboundCompleteResult(
        Inbound inbound,
        List<ReflectedInventory> inventory,
        Long purchaseOrderId,
        PurchaseOrderStatus purchaseOrderStatus,
        List<PurchaseOrderLineProgress> purchaseOrderLines
) {

    public record ReflectedInventory(
            Long inboundLineId,
            Long acceptedInventoryLotId,
            long acceptedQuantity,
            Long defectiveInventoryLotId,
            long defectiveQuantity
    ) {
    }

    public record PurchaseOrderLineProgress(
            Long purchaseOrderLineId,
            long expectedQuantity,
            long receivedQuantity,
            PurchaseOrderLineStatus status
    ) {
    }
}

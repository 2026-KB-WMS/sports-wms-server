package com.kb.wms.outbound.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.outbound.domain.enums.OutboundStatus;

/**
 * 피킹 완료 결과(PATCH /api/v1/outbounds/{outboundId}/picking/complete).
 *
 * @param hasShortage 부족분(할당 - 피킹)이 있는 항목이 하나라도 있는지
 */
public record OutboundPickingCompleteResult(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        boolean hasShortage,
        List<PickedItem> items,
        LocalDateTime updatedAt
) {

    /** 항목별 피킹 결과와 차감 뒤 재고 행 수량. */
    public record PickedItem(
            Long outboundLineId,
            Long allocationId,
            long allocatedQuantity,
            long pickedQuantity,
            BigDecimal confirmedUnitSupplyPrice,
            InventoryState inventory
    ) {

        public long shortageQuantity() {
            return allocatedQuantity - pickedQuantity;
        }

        public BigDecimal lineAmount() {
            return confirmedUnitSupplyPrice.multiply(BigDecimal.valueOf(pickedQuantity));
        }
    }

    public record InventoryState(Long inventoryLotId, long onHandQuantity, long allocatedQuantity) {
    }
}

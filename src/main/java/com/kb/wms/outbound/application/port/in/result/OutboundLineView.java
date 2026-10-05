package com.kb.wms.outbound.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 출고 항목 상세. 피킹 위치(로트·구역)와 할당 수량은 연결된 할당·재고 행에서 ID 기준으로 가져온다.
 * 단가·금액은 피킹 완료 전에는 null이다.
 */
public record OutboundLineView(
        Long outboundLineId,
        Long allocationId,
        Long storeOrderLineId,
        Long skuId,
        String skuCode,
        String skuName,
        String unit,
        Long inventoryLotId,
        Long lotId,
        String lotNumber,
        LocalDate expiryDate,
        Long sectionId,
        String sectionCode,
        Long allocatedQuantity,
        Long shippedQuantity,
        BigDecimal confirmedUnitSupplyPrice
) {

    /** 항목 금액(출고 수량 × 확정 공급 단가). 피킹 완료 전에는 null. */
    public BigDecimal lineAmount() {
        if (confirmedUnitSupplyPrice == null) {
            return null;
        }
        return confirmedUnitSupplyPrice.multiply(BigDecimal.valueOf(shippedQuantity));
    }
}
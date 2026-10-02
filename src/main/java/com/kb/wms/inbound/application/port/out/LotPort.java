package com.kb.wms.inbound.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 재고 도메인의 로트를 입고 도메인에서 쓰기 위한 아웃바운드 포트(ADR-004: 검수 시 로트 find-or-create).
 */
public interface LotPort {

    /**
     * SKU + 공급처 + 로트 번호로 로트를 찾고 없으면 만들어 로트 ID를 돌려준다.
     * 기존 로트의 원가·일자가 다르거나 가용 상태가 아니면 409
     * (LOT_UNIT_COST_MISMATCH / LOT_DATE_MISMATCH / LOT_NOT_AVAILABLE)를 던진다.
     */
    Long findOrRegisterLot(Long skuId, Long supplierId, String lotNumber,
                           LocalDate manufacturedDate, LocalDate expiryDate, BigDecimal unitCost);
}

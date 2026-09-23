package com.kb.wms.inventory.application.port.in.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 입고 검수 시 로트 find-or-create (ADR-004). SKU + 공급처 + 로트 번호가 같으면 기존 로트를 쓴다.
 */
public record LotRegisterCommand(
        Long skuId,
        Long supplierId,
        String lotNumber,
        LocalDate manufacturedDate,
        LocalDate expiryDate,
        BigDecimal unitCost
) {
}

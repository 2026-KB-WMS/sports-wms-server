package com.kb.wms.inventory.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.domain.enums.LotStatus;

/**
 * GET /api/v1/lots 목록 항목.
 * supplierName은 Supplier(입고 도메인) 테이블이 생기면 채운다. 지금은 supplierId만 내려준다.
 */
public record LotSummaryResponse(
        Long lotId,
        String lotNumber,
        Long skuId,
        String skuCode,
        String skuName,
        Long supplierId,
        LocalDate manufacturedDate,
        LocalDate expiryDate,
        LotStatus status,
        BigDecimal unitCost
) {

    public static LotSummaryResponse from(LotSummary summary) {
        return new LotSummaryResponse(
                summary.lotId(),
                summary.lotNumber(),
                summary.skuId(),
                summary.skuCode(),
                summary.skuName(),
                summary.supplierId(),
                summary.manufacturedDate(),
                summary.expiryDate(),
                summary.status(),
                summary.unitCost());
    }
}

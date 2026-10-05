package com.kb.wms.inventory.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.domain.enums.LotStatus;

/**
 * GET /api/v1/lots 목록 항목.
 */
public record LotSummaryResponse(
        Long lotId,
        String lotNumber,
        Long skuId,
        String skuCode,
        String skuName,
        Long supplierId,
        String supplierName,
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
                summary.supplierName(),
                summary.manufacturedDate(),
                summary.expiryDate(),
                summary.status(),
                summary.unitCost());
    }
}

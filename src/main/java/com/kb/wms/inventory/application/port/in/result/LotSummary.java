package com.kb.wms.inventory.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.inventory.domain.enums.LotStatus;

/**
 * 로트 마스터 + SKU 정보 (GET /api/v1/lots, /lots/{lotId}). 수량은 포함하지 않는다.
 * 공급처명은 Supplier(입고 도메인) 테이블이 생기면 추가한다.
 */
public record LotSummary(
        Long lotId,
        String lotNumber,
        Long skuId,
        String skuCode,
        String skuName,
        Long supplierId,
        LocalDate manufacturedDate,
        LocalDate expiryDate,
        LotStatus status,
        BigDecimal unitCost,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

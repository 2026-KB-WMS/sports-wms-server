package com.kb.wms.inbound.application.port.in.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * 발주(요청) 목록 한 행 (GET /api/v1/purchase-orders).
 * 공급처는 라인에 배정되므로 조회 쿼리가 아니라 서비스가 {@link #withSuppliers(List)}로 채운다(배정 전에는 빈 목록).
 *
 * @param lineCount   발주 항목 수
 * @param totalAmount 발주 항목 금액(line_amount) 합계
 * @param suppliers   라인에 배정된 공급처 목록(공급처 ID 오름차순)
 */
public record PurchaseOrderSummary(
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long warehouseId,
        String warehouseName,
        PurchaseOrderStatus status,
        LocalDateTime expectedAt,
        Long lineCount,
        BigDecimal totalAmount,
        Long createdBy,
        String createdByName,
        LocalDateTime createdAt,
        List<PurchaseOrderSupplierView> suppliers
) {

    /** 조회 쿼리(JPQL 생성자 표현식)용: 공급처 없이 만든다. */
    public PurchaseOrderSummary(Long purchaseOrderId, String purchaseOrderNo, Long warehouseId, String warehouseName,
                                PurchaseOrderStatus status, LocalDateTime expectedAt, Long lineCount,
                                BigDecimal totalAmount, Long createdBy, String createdByName,
                                LocalDateTime createdAt) {
        this(purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, status, expectedAt, lineCount,
                totalAmount, createdBy, createdByName, createdAt, List.of());
    }

    public PurchaseOrderSummary withSuppliers(List<PurchaseOrderSupplierView> suppliers) {
        return new PurchaseOrderSummary(purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, status,
                expectedAt, lineCount, totalAmount, createdBy, createdByName, createdAt, suppliers);
    }
}

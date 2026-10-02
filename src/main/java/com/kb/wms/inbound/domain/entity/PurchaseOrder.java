package com.kb.wms.inbound.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 창고가 공급업체에 상품을 주문하는 창고 발주 헤더.
 * 상태 전이: REQUESTED → CONFIRMED → COMPLETED, REQUESTED·CONFIRMED → CANCELED.
 * 발주 항목(PurchaseOrderLine)은 purchaseOrderId로 참조하며 이 객체가 직접 들고 있지 않는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrder {

    private Long purchaseOrderId;
    private String purchaseOrderNo;
    private Long warehouseId;
    private Long supplierId;
    private PurchaseOrderStatus status;
    private LocalDateTime expectedAt;
    private String note;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private PurchaseOrder(Long purchaseOrderId, String purchaseOrderNo, Long warehouseId, Long supplierId,
                          PurchaseOrderStatus status, LocalDateTime expectedAt, String note, Long createdBy,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.purchaseOrderId = purchaseOrderId;
        this.purchaseOrderNo = purchaseOrderNo;
        this.warehouseId = warehouseId;
        this.supplierId = supplierId;
        this.status = status == null ? PurchaseOrderStatus.REQUESTED : status;
        this.expectedAt = expectedAt;
        this.note = note;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PurchaseOrder register(String purchaseOrderNo, Long warehouseId, Long supplierId,
                                         LocalDateTime expectedAt, String note, Long createdBy) {
        return PurchaseOrder.builder()
                .purchaseOrderNo(purchaseOrderNo)
                .warehouseId(warehouseId)
                .supplierId(supplierId)
                .expectedAt(expectedAt)
                .note(note)
                .createdBy(createdBy)
                .status(PurchaseOrderStatus.REQUESTED)
                .build();
    }

    /** 본사 관리자 확정. REQUESTED에서만 가능하다. */
    public void confirm() {
        if (this.status != PurchaseOrderStatus.REQUESTED) {
            throw new IllegalStateException("요청 상태의 발주만 확정할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = PurchaseOrderStatus.CONFIRMED;
    }

    /** 취소. REQUESTED(작성자)·CONFIRMED(본사 관리자)에서만 가능하며, 권한 검사는 서비스에서 한다. */
    public void cancel() {
        if (this.status != PurchaseOrderStatus.REQUESTED && this.status != PurchaseOrderStatus.CONFIRMED) {
            throw new IllegalStateException("요청 또는 확정 상태의 발주만 취소할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = PurchaseOrderStatus.CANCELED;
    }

    /** 전량 입고 완료. 입고 완료 처리 시 시스템이 호출하며 CONFIRMED에서만 가능하다. */
    public void complete() {
        if (this.status != PurchaseOrderStatus.CONFIRMED) {
            throw new IllegalStateException("확정 상태의 발주만 완료 처리할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = PurchaseOrderStatus.COMPLETED;
    }

    public boolean isRequested() {
        return this.status == PurchaseOrderStatus.REQUESTED;
    }

    public boolean isConfirmed() {
        return this.status == PurchaseOrderStatus.CONFIRMED;
    }

    /** 진행 중인 발주(REQUESTED·CONFIRMED). 공급처 비활성화 가능 여부 판단 기준이다. */
    public boolean isInProgress() {
        return isRequested() || isConfirmed();
    }
}

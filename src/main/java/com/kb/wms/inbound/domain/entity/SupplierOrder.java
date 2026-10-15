package com.kb.wms.inbound.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 본사가 공급처에 내는 발주서. 창고 발주 요청(PurchaseOrder)을 확정할 때 공급처당 한 건이 만들어진다.
 * 상태 전이: ISSUED → PARTIALLY_RECEIVED → COMPLETED, ISSUED·PARTIALLY_RECEIVED → CANCELED.
 * 발주서 상세 테이블은 따로 없고, 요청 라인(PurchaseOrderLine)이 supplierOrderId로 이 발주서에 연결된다.
 * (purchaseOrderId, supplierId) 쌍은 유일하다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierOrder {

    private Long supplierOrderId;
    private String supplierOrderNo;
    private Long purchaseOrderId;
    private Long supplierId;
    private SupplierOrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private SupplierOrder(Long supplierOrderId, String supplierOrderNo, Long purchaseOrderId, Long supplierId,
                          SupplierOrderStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.supplierOrderId = supplierOrderId;
        this.supplierOrderNo = supplierOrderNo;
        this.purchaseOrderId = purchaseOrderId;
        this.supplierId = supplierId;
        this.status = status == null ? SupplierOrderStatus.ISSUED : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 요청 확정 시 공급처별로 발주서를 발행한다. 번호는 호출 측(서비스)이 생성해 넘긴다. */
    public static SupplierOrder issue(String supplierOrderNo, Long purchaseOrderId, Long supplierId) {
        if (supplierOrderNo == null || supplierOrderNo.isBlank()) {
            throw new IllegalArgumentException("공급처 발주서 번호는 필수입니다.");
        }
        if (purchaseOrderId == null || supplierId == null) {
            throw new IllegalArgumentException("발주 요청 ID와 공급처 ID는 필수입니다.");
        }
        return SupplierOrder.builder()
                .supplierOrderNo(supplierOrderNo)
                .purchaseOrderId(purchaseOrderId)
                .supplierId(supplierId)
                .status(SupplierOrderStatus.ISSUED)
                .build();
    }

    /** 첫 입고 완료 후에도 소속 라인이 남아 있을 때. ISSUED에서만 전이하며, 이미 PARTIALLY_RECEIVED면 상태를 유지한다. */
    public void markPartiallyReceived() {
        if (this.status == SupplierOrderStatus.PARTIALLY_RECEIVED) {
            return;
        }
        if (this.status != SupplierOrderStatus.ISSUED) {
            throw new IllegalStateException("발행 또는 일부 입고 상태의 발주서만 일부 입고 처리할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = SupplierOrderStatus.PARTIALLY_RECEIVED;
    }

    /** 소속 라인이 모두 입고 완료됐을 때 시스템이 호출한다. ISSUED·PARTIALLY_RECEIVED에서만 가능하다. */
    public void complete() {
        if (!isInProgress()) {
            throw new IllegalStateException("발행 또는 일부 입고 상태의 발주서만 완료 처리할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = SupplierOrderStatus.COMPLETED;
    }

    /** 요청 취소의 전파. ISSUED·PARTIALLY_RECEIVED에서만 가능하다. 단독 취소 API는 없다. */
    public void cancel() {
        if (!isInProgress()) {
            throw new IllegalStateException("발행 또는 일부 입고 상태의 발주서만 취소할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = SupplierOrderStatus.CANCELED;
    }

    /** 진행 중인 발주서(ISSUED·PARTIALLY_RECEIVED). 입고 등록과 공급처 비활성화 가드의 기준이다. */
    public boolean isInProgress() {
        return this.status == SupplierOrderStatus.ISSUED || this.status == SupplierOrderStatus.PARTIALLY_RECEIVED;
    }
}

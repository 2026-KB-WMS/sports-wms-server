package com.kb.wms.inbound.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 창고 발주의 SKU별 예정 수량 상세.
 * 입고가 완료될 때마다 receive()로 입고 수량을 누적하며, 상태는
 * REQUESTED → PARTIALLY_RECEIVED → COMPLETED로 갱신된다.
 * 발주 번호당 SKU는 한 줄만 둘 수 있다 (UNIQUE(purchase_order_id, sku_id)).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderLine {

    private Long purchaseOrderLineId;
    private Long purchaseOrderId;
    private Long skuId;
    private Long supplierId;
    private Long supplierOrderId;
    private Long expectedQuantity;
    private Long receivedQuantity;
    private BigDecimal lineAmount;
    private BigDecimal orderedUnitPrice;
    private PurchaseOrderLineStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private PurchaseOrderLine(Long purchaseOrderLineId, Long purchaseOrderId, Long skuId, Long supplierId,
                              Long supplierOrderId, Long expectedQuantity, Long receivedQuantity, BigDecimal lineAmount,
                              BigDecimal orderedUnitPrice, PurchaseOrderLineStatus status,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.purchaseOrderLineId = purchaseOrderLineId;
        this.purchaseOrderId = purchaseOrderId;
        this.skuId = skuId;
        this.supplierId = supplierId;
        this.supplierOrderId = supplierOrderId;
        this.expectedQuantity = expectedQuantity;
        this.receivedQuantity = receivedQuantity == null ? 0L : receivedQuantity;
        this.lineAmount = lineAmount;
        this.orderedUnitPrice = orderedUnitPrice;
        this.status = status == null ? PurchaseOrderLineStatus.REQUESTED : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 발주 항목 등록. 총 발주 금액은 발주 수량 × 매입 단가 스냅샷으로 계산한다. */
    public static PurchaseOrderLine register(Long purchaseOrderId, Long skuId, long expectedQuantity,
                                             BigDecimal orderedUnitPrice) {
        if (expectedQuantity <= 0) {
            throw new IllegalArgumentException("발주 수량은 1 이상이어야 합니다.");
        }
        if (orderedUnitPrice == null || orderedUnitPrice.signum() < 0) {
            throw new IllegalArgumentException("매입 단가는 0 이상이어야 합니다.");
        }
        return PurchaseOrderLine.builder()
                .purchaseOrderId(purchaseOrderId)
                .skuId(skuId)
                .expectedQuantity(expectedQuantity)
                .orderedUnitPrice(orderedUnitPrice)
                .lineAmount(orderedUnitPrice.multiply(BigDecimal.valueOf(expectedQuantity)))
                .status(PurchaseOrderLineStatus.REQUESTED)
                .build();
    }

    /**
     * 입고 수량을 누적하고 상태를 갱신한다. 누적 수량이 발주 수량 미만이면 PARTIALLY_RECEIVED,
     * 이상이면 COMPLETED. 초과 입고 허용 여부는 입고 서비스에서 판단한다.
     */
    public void receive(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("입고 수량은 1 이상이어야 합니다.");
        }
        if (this.status == PurchaseOrderLineStatus.COMPLETED) {
            throw new IllegalStateException("이미 전량 입고된 발주 항목입니다.");
        }
        this.receivedQuantity += quantity;
        this.status = this.receivedQuantity >= this.expectedQuantity
                ? PurchaseOrderLineStatus.COMPLETED
                : PurchaseOrderLineStatus.PARTIALLY_RECEIVED;
    }

    /**
     * 본사 관리자가 확정할 때 라인의 공급처를 지정한다. 입고가 시작되기 전(REQUESTED)에만 지정·변경할 수 있다.
     * 해당 SKU에 매핑된 공급처인지는 서비스에서 확인한다.
     */
    public void assignSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("공급처 ID는 필수입니다.");
        }
        if (this.status != PurchaseOrderLineStatus.REQUESTED) {
            throw new IllegalStateException("입고가 시작된 발주 항목의 공급처는 바꿀 수 없습니다. 현재 상태: " + this.status);
        }
        this.supplierId = supplierId;
    }

    /**
     * 확정 시 공급처 발주서가 만들어지면 라인을 발주서에 연결한다. 공급처가 배정된 라인만 연결할 수 있고 한 번만 연결한다.
     * 발주서의 공급처와 라인의 공급처가 같은지는 서비스에서 확인한다.
     */
    public void attachToSupplierOrder(Long supplierOrderId) {
        if (supplierOrderId == null) {
            throw new IllegalArgumentException("공급처 발주서 ID는 필수입니다.");
        }
        if (this.supplierId == null) {
            throw new IllegalStateException("공급처가 배정되지 않은 발주 항목은 공급처 발주서에 연결할 수 없습니다.");
        }
        if (this.supplierOrderId != null) {
            throw new IllegalStateException("이미 공급처 발주서에 연결된 발주 항목입니다.");
        }
        this.supplierOrderId = supplierOrderId;
    }

    public boolean isCompleted() {
        return this.status == PurchaseOrderLineStatus.COMPLETED;
    }

    /** 아직 입고되지 않은 수량. 초과 입고여도 음수가 되지 않는다. */
    public long remainingQuantity() {
        return Math.max(0L, this.expectedQuantity - this.receivedQuantity);
    }
}

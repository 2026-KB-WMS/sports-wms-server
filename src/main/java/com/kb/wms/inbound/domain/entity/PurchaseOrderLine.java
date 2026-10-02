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
    private Long expectedQuantity;
    private Long receivedQuantity;
    private BigDecimal lineAmount;
    private BigDecimal orderedUnitPrice;
    private PurchaseOrderLineStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private PurchaseOrderLine(Long purchaseOrderLineId, Long purchaseOrderId, Long skuId, Long expectedQuantity,
                              Long receivedQuantity, BigDecimal lineAmount, BigDecimal orderedUnitPrice,
                              PurchaseOrderLineStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.purchaseOrderLineId = purchaseOrderLineId;
        this.purchaseOrderId = purchaseOrderId;
        this.skuId = skuId;
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

    public boolean isCompleted() {
        return this.status == PurchaseOrderLineStatus.COMPLETED;
    }

    /** 아직 입고되지 않은 수량. 초과 입고여도 음수가 되지 않는다. */
    public long remainingQuantity() {
        return Math.max(0L, this.expectedQuantity - this.receivedQuantity);
    }
}

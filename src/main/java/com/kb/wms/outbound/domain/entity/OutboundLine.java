package com.kb.wms.outbound.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 출고 항목. 재고 할당 하나와 1:1 로 연결된다.
 * 피킹 완료 시 출고 수량과 확정 공급 단가가 기록된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboundLine {

    private Long outboundLineId;
    private Long outboundId;
    private Long allocationId;
    private long shippedQuantity;
    private BigDecimal confirmedUnitSupplyPrice;
    private LocalDateTime createdAt;

    @Builder
    private OutboundLine(Long outboundLineId, Long outboundId, Long allocationId,
                         long shippedQuantity, BigDecimal confirmedUnitSupplyPrice,
                         LocalDateTime createdAt) {
        this.outboundLineId = outboundLineId;
        this.outboundId = outboundId;
        this.allocationId = allocationId;
        this.shippedQuantity = shippedQuantity;
        this.confirmedUnitSupplyPrice = confirmedUnitSupplyPrice;
        this.createdAt = createdAt;
    }

    public static OutboundLine create(Long outboundId, Long allocationId) {
        if (allocationId == null) {
            throw new IllegalArgumentException("재고 할당은 필수입니다.");
        }
        return OutboundLine.builder()
                .outboundId(outboundId)
                .allocationId(allocationId)
                .shippedQuantity(0)
                .build();
    }

    /** 피킹 완료 확정. 한 번만 가능하며 수량은 0 이상 할당 수량 이하. */
    public void confirmPicking(long pickedQuantity, long allocatedQuantity, BigDecimal unitSupplyPrice) {
        if (confirmedUnitSupplyPrice != null) {
            throw new IllegalStateException("이미 피킹이 확정된 출고 항목입니다.");
        }
        if (pickedQuantity < 0 || pickedQuantity > allocatedQuantity) {
            throw new IllegalArgumentException("피킹 수량은 0 이상, 할당 수량 이하여야 합니다.");
        }
        if (unitSupplyPrice == null || unitSupplyPrice.signum() < 0) {
            throw new IllegalArgumentException("공급 단가는 0 이상이어야 합니다.");
        }
        this.shippedQuantity = pickedQuantity;
        this.confirmedUnitSupplyPrice = unitSupplyPrice;
    }

    public boolean isPickingConfirmed() {
        return confirmedUnitSupplyPrice != null;
    }

    /** 항목 금액 (단가 × 수량). 피킹 확정 전에는 null. */
    public BigDecimal lineAmount() {
        if (confirmedUnitSupplyPrice == null) {
            return null;
        }
        return confirmedUnitSupplyPrice.multiply(BigDecimal.valueOf(shippedQuantity));
    }
}

package com.kb.wms.inventory.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모든 재고 보유 수량(on_hand) 변동을 남기는 감사 이력이다. 생성 후 변경하지 않는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryTransaction {

    private Long transactionId;
    private Long inventoryLotId;
    private TransactionType transactionType;
    private Long quantityDelta;
    private Long beforeQuantity;
    private Long afterQuantity;
    private ReferenceType referenceType;
    private Long referenceId;
    private String reason;
    private Long createdBy;
    private LocalDateTime createdAt;

    @Builder
    private InventoryTransaction(Long transactionId, Long inventoryLotId, TransactionType transactionType,
                                 Long quantityDelta, Long beforeQuantity, Long afterQuantity,
                                 ReferenceType referenceType, Long referenceId, String reason,
                                 Long createdBy, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.inventoryLotId = inventoryLotId;
        this.transactionType = transactionType;
        this.quantityDelta = quantityDelta;
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    /**
     * 보유 수량 변동 이력을 기록한다. 변동 수량은 before/after 차이로 계산한다.
     *
     * @param beforeQuantity 변동 전 보유 수량
     * @param afterQuantity  변동 후 보유 수량
     * @param referenceId    원천 문서 ID (재고 조정처럼 원천 문서가 없으면 null)
     */
    public static InventoryTransaction record(Long inventoryLotId, TransactionType transactionType,
                                              long beforeQuantity, long afterQuantity,
                                              ReferenceType referenceType, Long referenceId,
                                              String reason, Long createdBy) {
        return InventoryTransaction.builder()
                .inventoryLotId(inventoryLotId)
                .transactionType(transactionType)
                .quantityDelta(afterQuantity - beforeQuantity)
                .beforeQuantity(beforeQuantity)
                .afterQuantity(afterQuantity)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .reason(reason)
                .createdBy(createdBy)
                .build();
    }
}

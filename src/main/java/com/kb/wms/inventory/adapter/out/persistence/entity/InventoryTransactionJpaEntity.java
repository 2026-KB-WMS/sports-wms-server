package com.kb.wms.inventory.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.kb.wms.inventory.domain.entity.InventoryTransaction;
import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 변경 없는 감사 이력이라 updated_at이 없어 BaseTimeEntity를 상속하지 않는다.
 */
@Entity
@Table(name = "inventory_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryTransactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "inventory_lot_id", nullable = false)
    private Long inventoryLotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(name = "quantity_delta", nullable = false)
    private Long quantityDelta;

    @Column(name = "before_quantity", nullable = false)
    private Long beforeQuantity;

    @Column(name = "after_quantity", nullable = false)
    private Long afterQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private ReferenceType referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private InventoryTransactionJpaEntity(Long transactionId, Long inventoryLotId, TransactionType transactionType,
                                          Long quantityDelta, Long beforeQuantity, Long afterQuantity,
                                          ReferenceType referenceType, Long referenceId, String reason,
                                          Long createdBy) {
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
    }

    public static InventoryTransactionJpaEntity fromDomain(InventoryTransaction transaction) {
        return InventoryTransactionJpaEntity.builder()
                .transactionId(transaction.getTransactionId())
                .inventoryLotId(transaction.getInventoryLotId())
                .transactionType(transaction.getTransactionType())
                .quantityDelta(transaction.getQuantityDelta())
                .beforeQuantity(transaction.getBeforeQuantity())
                .afterQuantity(transaction.getAfterQuantity())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .reason(transaction.getReason())
                .createdBy(transaction.getCreatedBy())
                .build();
    }

    public InventoryTransaction toDomain() {
        return InventoryTransaction.builder()
                .transactionId(transactionId)
                .inventoryLotId(inventoryLotId)
                .transactionType(transactionType)
                .quantityDelta(quantityDelta)
                .beforeQuantity(beforeQuantity)
                .afterQuantity(afterQuantity)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .reason(reason)
                .createdBy(createdBy)
                .createdAt(createdAt)
                .build();
    }
}

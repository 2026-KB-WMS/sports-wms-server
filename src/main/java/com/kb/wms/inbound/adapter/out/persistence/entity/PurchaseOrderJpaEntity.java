package com.kb.wms.inbound.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "purchase_order",
        uniqueConstraints = @UniqueConstraint(name = "uk_purchase_order_no", columnNames = "purchase_order_no"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @Column(name = "purchase_order_no", nullable = false, length = 50)
    private String purchaseOrderNo;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseOrderStatus status;

    @Column(name = "expected_at")
    private LocalDateTime expectedAt;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Builder
    private PurchaseOrderJpaEntity(Long purchaseOrderId, String purchaseOrderNo, Long warehouseId, Long supplierId,
                                   PurchaseOrderStatus status, LocalDateTime expectedAt, String note,
                                   Long createdBy) {
        this.purchaseOrderId = purchaseOrderId;
        this.purchaseOrderNo = purchaseOrderNo;
        this.warehouseId = warehouseId;
        this.supplierId = supplierId;
        this.status = status;
        this.expectedAt = expectedAt;
        this.note = note;
        this.createdBy = createdBy;
    }

    public static PurchaseOrderJpaEntity fromDomain(PurchaseOrder purchaseOrder) {
        return PurchaseOrderJpaEntity.builder()
                .purchaseOrderId(purchaseOrder.getPurchaseOrderId())
                .purchaseOrderNo(purchaseOrder.getPurchaseOrderNo())
                .warehouseId(purchaseOrder.getWarehouseId())
                .supplierId(purchaseOrder.getSupplierId())
                .status(purchaseOrder.getStatus())
                .expectedAt(purchaseOrder.getExpectedAt())
                .note(purchaseOrder.getNote())
                .createdBy(purchaseOrder.getCreatedBy())
                .build();
    }

    public PurchaseOrder toDomain() {
        return PurchaseOrder.builder()
                .purchaseOrderId(purchaseOrderId)
                .purchaseOrderNo(purchaseOrderNo)
                .warehouseId(warehouseId)
                .supplierId(supplierId)
                .status(status)
                .expectedAt(expectedAt)
                .note(note)
                .createdBy(createdBy)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

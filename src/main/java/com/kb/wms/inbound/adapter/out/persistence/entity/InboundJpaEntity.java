package com.kb.wms.inbound.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.enums.InboundStatus;

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
@Table(name = "inbound",
        uniqueConstraints = @UniqueConstraint(name = "uk_inbound_no", columnNames = "inbound_no"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboundJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbound_id")
    private Long inboundId;

    @Column(name = "inbound_no", nullable = false, length = 50)
    private String inboundNo;

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InboundStatus status;

    @Column(name = "arrived_at", nullable = false)
    private LocalDateTime arrivedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(name = "note", length = 1000)
    private String note;

    @Builder
    private InboundJpaEntity(Long inboundId, String inboundNo, Long purchaseOrderId, Long warehouseId,
                             InboundStatus status, LocalDateTime arrivedAt, LocalDateTime receivedAt,
                             Long receivedBy, String note) {
        this.inboundId = inboundId;
        this.inboundNo = inboundNo;
        this.purchaseOrderId = purchaseOrderId;
        this.warehouseId = warehouseId;
        this.status = status;
        this.arrivedAt = arrivedAt;
        this.receivedAt = receivedAt;
        this.receivedBy = receivedBy;
        this.note = note;
    }

    public static InboundJpaEntity fromDomain(Inbound inbound) {
        return InboundJpaEntity.builder()
                .inboundId(inbound.getInboundId())
                .inboundNo(inbound.getInboundNo())
                .purchaseOrderId(inbound.getPurchaseOrderId())
                .warehouseId(inbound.getWarehouseId())
                .status(inbound.getStatus())
                .arrivedAt(inbound.getArrivedAt())
                .receivedAt(inbound.getReceivedAt())
                .receivedBy(inbound.getReceivedBy())
                .note(inbound.getNote())
                .build();
    }

    public Inbound toDomain() {
        return Inbound.builder()
                .inboundId(inboundId)
                .inboundNo(inboundNo)
                .purchaseOrderId(purchaseOrderId)
                .warehouseId(warehouseId)
                .status(status)
                .arrivedAt(arrivedAt)
                .receivedAt(receivedAt)
                .receivedBy(receivedBy)
                .note(note)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

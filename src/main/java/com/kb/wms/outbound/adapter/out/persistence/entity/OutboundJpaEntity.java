package com.kb.wms.outbound.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

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
@Table(name = "outbound",
        uniqueConstraints = @UniqueConstraint(name = "uk_outbound_no", columnNames = "outbound_no"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboundJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbound_id")
    private Long outboundId;

    @Column(name = "outbound_no", nullable = false, length = 30)
    private String outboundNo;

    @Column(name = "store_order_id", nullable = false)
    private Long storeOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboundStatus status;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "shipped_by")
    private Long shippedBy;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "note", length = 500)
    private String note;

    @Builder
    private OutboundJpaEntity(Long outboundId, String outboundNo, Long storeOrderId, OutboundStatus status,
                              LocalDateTime shippedAt, Long shippedBy, LocalDateTime deliveredAt, String note) {
        this.outboundId = outboundId;
        this.outboundNo = outboundNo;
        this.storeOrderId = storeOrderId;
        this.status = status;
        this.shippedAt = shippedAt;
        this.shippedBy = shippedBy;
        this.deliveredAt = deliveredAt;
        this.note = note;
    }

    public static OutboundJpaEntity fromDomain(Outbound outbound) {
        return OutboundJpaEntity.builder()
                .outboundId(outbound.getOutboundId())
                .outboundNo(outbound.getOutboundNo())
                .storeOrderId(outbound.getStoreOrderId())
                .status(outbound.getStatus())
                .shippedAt(outbound.getShippedAt())
                .shippedBy(outbound.getShippedBy())
                .deliveredAt(outbound.getDeliveredAt())
                .note(outbound.getNote())
                .build();
    }

    public Outbound toDomain() {
        return Outbound.builder()
                .outboundId(outboundId)
                .outboundNo(outboundNo)
                .storeOrderId(storeOrderId)
                .status(status)
                .shippedAt(shippedAt)
                .shippedBy(shippedBy)
                .deliveredAt(deliveredAt)
                .note(note)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

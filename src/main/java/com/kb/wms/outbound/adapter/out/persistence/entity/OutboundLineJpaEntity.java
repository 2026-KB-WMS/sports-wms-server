package com.kb.wms.outbound.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.kb.wms.outbound.domain.entity.OutboundLine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** outbound_line 테이블에는 updated_at 컬럼이 없어 BaseTimeEntity 대신 created_at만 직접 관리한다. */
@Entity
@Table(name = "outbound_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_outbound_line_outbound_allocation",
                columnNames = {"outbound_id", "allocation_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboundLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbound_line_id")
    private Long outboundLineId;

    @Column(name = "outbound_id", nullable = false)
    private Long outboundId;

    @Column(name = "allocation_id", nullable = false)
    private Long allocationId;

    @Column(name = "shipped_quantity", nullable = false)
    private Long shippedQuantity;

    @Column(name = "confirmed_unit_supply_price", precision = 18, scale = 2)
    private BigDecimal confirmedUnitSupplyPrice;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private OutboundLineJpaEntity(Long outboundLineId, Long outboundId, Long allocationId,
                                  Long shippedQuantity, BigDecimal confirmedUnitSupplyPrice) {
        this.outboundLineId = outboundLineId;
        this.outboundId = outboundId;
        this.allocationId = allocationId;
        this.shippedQuantity = shippedQuantity;
        this.confirmedUnitSupplyPrice = confirmedUnitSupplyPrice;
    }

    public static OutboundLineJpaEntity fromDomain(OutboundLine line) {
        return OutboundLineJpaEntity.builder()
                .outboundLineId(line.getOutboundLineId())
                .outboundId(line.getOutboundId())
                .allocationId(line.getAllocationId())
                .shippedQuantity(line.getShippedQuantity())
                .confirmedUnitSupplyPrice(line.getConfirmedUnitSupplyPrice())
                .build();
    }

    public OutboundLine toDomain() {
        return OutboundLine.builder()
                .outboundLineId(outboundLineId)
                .outboundId(outboundId)
                .allocationId(allocationId)
                .shippedQuantity(shippedQuantity)
                .confirmedUnitSupplyPrice(confirmedUnitSupplyPrice)
                .createdAt(createdAt)
                .build();
    }
}

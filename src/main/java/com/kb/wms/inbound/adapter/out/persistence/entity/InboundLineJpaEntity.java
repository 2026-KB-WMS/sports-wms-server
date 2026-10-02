package com.kb.wms.inbound.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.kb.wms.inbound.domain.entity.InboundLine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 검수(inspect) 때마다 항목을 통째로 교체하고 updated_at 컬럼이 없어 BaseTimeEntity를 상속하지 않는다.
 */
@Entity
@Table(name = "inbound_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_inbound_line_inbound_po_line_lot",
                columnNames = {"inbound_id", "purchase_order_line_id", "lot_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboundLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbound_line_id")
    private Long inboundLineId;

    @Column(name = "inbound_id", nullable = false)
    private Long inboundId;

    @Column(name = "purchase_order_line_id", nullable = false)
    private Long purchaseOrderLineId;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "accepted_section_id")
    private Long acceptedSectionId;

    @Column(name = "defect_section_id")
    private Long defectSectionId;

    @Column(name = "received_quantity", nullable = false)
    private Long receivedQuantity;

    @Column(name = "accepted_quantity", nullable = false)
    private Long acceptedQuantity;

    @Column(name = "defective_quantity", nullable = false)
    private Long defectiveQuantity;

    @Column(name = "received_unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal receivedUnitPrice;

    @Column(name = "line_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineAmount;

    @Column(name = "price_change_reason", length = 500)
    private String priceChangeReason;

    @Column(name = "inspection_note", length = 1000)
    private String inspectionNote;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "received_by", nullable = false)
    private Long receivedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private InboundLineJpaEntity(Long inboundLineId, Long inboundId, Long purchaseOrderLineId, Long lotId,
                                 Long acceptedSectionId, Long defectSectionId, Long receivedQuantity,
                                 Long acceptedQuantity, Long defectiveQuantity, BigDecimal receivedUnitPrice,
                                 BigDecimal lineAmount, String priceChangeReason, String inspectionNote,
                                 LocalDateTime receivedAt, Long receivedBy) {
        this.inboundLineId = inboundLineId;
        this.inboundId = inboundId;
        this.purchaseOrderLineId = purchaseOrderLineId;
        this.lotId = lotId;
        this.acceptedSectionId = acceptedSectionId;
        this.defectSectionId = defectSectionId;
        this.receivedQuantity = receivedQuantity;
        this.acceptedQuantity = acceptedQuantity;
        this.defectiveQuantity = defectiveQuantity;
        this.receivedUnitPrice = receivedUnitPrice;
        this.lineAmount = lineAmount;
        this.priceChangeReason = priceChangeReason;
        this.inspectionNote = inspectionNote;
        this.receivedAt = receivedAt;
        this.receivedBy = receivedBy;
    }

    public static InboundLineJpaEntity fromDomain(InboundLine line) {
        return InboundLineJpaEntity.builder()
                .inboundLineId(line.getInboundLineId())
                .inboundId(line.getInboundId())
                .purchaseOrderLineId(line.getPurchaseOrderLineId())
                .lotId(line.getLotId())
                .acceptedSectionId(line.getAcceptedSectionId())
                .defectSectionId(line.getDefectSectionId())
                .receivedQuantity(line.getReceivedQuantity())
                .acceptedQuantity(line.getAcceptedQuantity())
                .defectiveQuantity(line.getDefectiveQuantity())
                .receivedUnitPrice(line.getReceivedUnitPrice())
                .lineAmount(line.getLineAmount())
                .priceChangeReason(line.getPriceChangeReason())
                .inspectionNote(line.getInspectionNote())
                .receivedAt(line.getReceivedAt())
                .receivedBy(line.getReceivedBy())
                .build();
    }

    public InboundLine toDomain() {
        return InboundLine.builder()
                .inboundLineId(inboundLineId)
                .inboundId(inboundId)
                .purchaseOrderLineId(purchaseOrderLineId)
                .lotId(lotId)
                .acceptedSectionId(acceptedSectionId)
                .defectSectionId(defectSectionId)
                .receivedQuantity(receivedQuantity)
                .acceptedQuantity(acceptedQuantity)
                .defectiveQuantity(defectiveQuantity)
                .receivedUnitPrice(receivedUnitPrice)
                .lineAmount(lineAmount)
                .priceChangeReason(priceChangeReason)
                .inspectionNote(inspectionNote)
                .receivedAt(receivedAt)
                .receivedBy(receivedBy)
                .createdAt(createdAt)
                .build();
    }
}

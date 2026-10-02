package com.kb.wms.inbound.adapter.out.persistence.entity;

import java.math.BigDecimal;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;

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
@Table(name = "purchase_order_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_purchase_order_line_order_sku",
                columnNames = {"purchase_order_id", "sku_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderLineJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_order_line_id")
    private Long purchaseOrderLineId;

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "expected_quantity", nullable = false)
    private Long expectedQuantity;

    @Column(name = "received_quantity", nullable = false)
    private Long receivedQuantity;

    @Column(name = "line_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineAmount;

    @Column(name = "ordered_unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal orderedUnitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseOrderLineStatus status;

    @Builder
    private PurchaseOrderLineJpaEntity(Long purchaseOrderLineId, Long purchaseOrderId, Long skuId,
                                       Long expectedQuantity, Long receivedQuantity, BigDecimal lineAmount,
                                       BigDecimal orderedUnitPrice, PurchaseOrderLineStatus status) {
        this.purchaseOrderLineId = purchaseOrderLineId;
        this.purchaseOrderId = purchaseOrderId;
        this.skuId = skuId;
        this.expectedQuantity = expectedQuantity;
        this.receivedQuantity = receivedQuantity;
        this.lineAmount = lineAmount;
        this.orderedUnitPrice = orderedUnitPrice;
        this.status = status;
    }

    public static PurchaseOrderLineJpaEntity fromDomain(PurchaseOrderLine line) {
        return PurchaseOrderLineJpaEntity.builder()
                .purchaseOrderLineId(line.getPurchaseOrderLineId())
                .purchaseOrderId(line.getPurchaseOrderId())
                .skuId(line.getSkuId())
                .expectedQuantity(line.getExpectedQuantity())
                .receivedQuantity(line.getReceivedQuantity())
                .lineAmount(line.getLineAmount())
                .orderedUnitPrice(line.getOrderedUnitPrice())
                .status(line.getStatus())
                .build();
    }

    public PurchaseOrderLine toDomain() {
        return PurchaseOrderLine.builder()
                .purchaseOrderLineId(purchaseOrderLineId)
                .purchaseOrderId(purchaseOrderId)
                .skuId(skuId)
                .expectedQuantity(expectedQuantity)
                .receivedQuantity(receivedQuantity)
                .lineAmount(lineAmount)
                .orderedUnitPrice(orderedUnitPrice)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

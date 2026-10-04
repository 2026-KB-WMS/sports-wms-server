package com.kb.wms.storeorder.adapter.out.persistence.entity;

import java.math.BigDecimal;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;

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
@Table(name = "store_order_line",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_order_line_order_sku",
                columnNames = {"store_order_id", "sku_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOrderLineJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_order_line_id")
    private Long storeOrderLineId;

    @Column(name = "store_order_id", nullable = false)
    private Long storeOrderId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "requested_quantity", nullable = false)
    private Long requestedQuantity;

    @Column(name = "allocated_quantity", nullable = false)
    private Long allocatedQuantity;

    @Column(name = "shipped_quantity", nullable = false)
    private Long shippedQuantity;

    @Column(name = "requested_unit_supply_price", precision = 18, scale = 2)
    private BigDecimal requestedUnitSupplyPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreOrderLineStatus status;

    @Builder
    private StoreOrderLineJpaEntity(Long storeOrderLineId, Long storeOrderId, Long skuId, Long requestedQuantity,
                                    Long allocatedQuantity, Long shippedQuantity,
                                    BigDecimal requestedUnitSupplyPrice, StoreOrderLineStatus status) {
        this.storeOrderLineId = storeOrderLineId;
        this.storeOrderId = storeOrderId;
        this.skuId = skuId;
        this.requestedQuantity = requestedQuantity;
        this.allocatedQuantity = allocatedQuantity;
        this.shippedQuantity = shippedQuantity;
        this.requestedUnitSupplyPrice = requestedUnitSupplyPrice;
        this.status = status;
    }

    public static StoreOrderLineJpaEntity fromDomain(StoreOrderLine line) {
        return StoreOrderLineJpaEntity.builder()
                .storeOrderLineId(line.getStoreOrderLineId())
                .storeOrderId(line.getStoreOrderId())
                .skuId(line.getSkuId())
                .requestedQuantity(line.getRequestedQuantity())
                .allocatedQuantity(line.getAllocatedQuantity())
                .shippedQuantity(line.getShippedQuantity())
                .requestedUnitSupplyPrice(line.getRequestedUnitSupplyPrice())
                .status(line.getStatus())
                .build();
    }

    public StoreOrderLine toDomain() {
        return StoreOrderLine.builder()
                .storeOrderLineId(storeOrderLineId)
                .storeOrderId(storeOrderId)
                .skuId(skuId)
                .requestedQuantity(requestedQuantity)
                .allocatedQuantity(allocatedQuantity)
                .shippedQuantity(shippedQuantity)
                .requestedUnitSupplyPrice(requestedUnitSupplyPrice)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

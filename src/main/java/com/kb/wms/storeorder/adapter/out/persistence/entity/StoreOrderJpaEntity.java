package com.kb.wms.storeorder.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

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
@Table(name = "store_order",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_order_no", columnNames = "order_no"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOrderJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_order_id")
    private Long storeOrderId;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreOrderStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "requested_delivery_at")
    private LocalDateTime requestedDeliveryAt;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Builder
    private StoreOrderJpaEntity(Long storeOrderId, String orderNo, Long storeId, Long warehouseId,
                                StoreOrderStatus status, LocalDateTime requestedAt,
                                LocalDateTime requestedDeliveryAt, String note, Long createdBy) {
        this.storeOrderId = storeOrderId;
        this.orderNo = orderNo;
        this.storeId = storeId;
        this.warehouseId = warehouseId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.requestedDeliveryAt = requestedDeliveryAt;
        this.note = note;
        this.createdBy = createdBy;
    }

    public static StoreOrderJpaEntity fromDomain(StoreOrder order) {
        return StoreOrderJpaEntity.builder()
                .storeOrderId(order.getStoreOrderId())
                .orderNo(order.getOrderNo())
                .storeId(order.getStoreId())
                .warehouseId(order.getWarehouseId())
                .status(order.getStatus())
                .requestedAt(order.getRequestedAt())
                .requestedDeliveryAt(order.getRequestedDeliveryAt())
                .note(order.getNote())
                .createdBy(order.getCreatedBy())
                .build();
    }

    public StoreOrder toDomain() {
        return StoreOrder.builder()
                .storeOrderId(storeOrderId)
                .orderNo(orderNo)
                .storeId(storeId)
                .warehouseId(warehouseId)
                .status(status)
                .requestedAt(requestedAt)
                .requestedDeliveryAt(requestedDeliveryAt)
                .note(note)
                .createdBy(createdBy)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

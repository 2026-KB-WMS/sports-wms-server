package com.kb.wms.outbound.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;

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

@Entity
@Table(name = "stock_allocation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockAllocationJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Long allocationId;

    @Column(name = "store_order_line_id", nullable = false)
    private Long storeOrderLineId;

    @Column(name = "inventory_lot_id", nullable = false)
    private Long inventoryLotId;

    @Column(name = "allocated_quantity", nullable = false)
    private Long allocatedQuantity;

    @Column(name = "picked_quantity", nullable = false)
    private Long pickedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AllocationStatus status;

    @Column(name = "allocated_at", nullable = false)
    private LocalDateTime allocatedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "allocated_by")
    private Long allocatedBy;

    @Builder
    private StockAllocationJpaEntity(Long allocationId, Long storeOrderLineId, Long inventoryLotId,
                                     Long allocatedQuantity, Long pickedQuantity, AllocationStatus status,
                                     LocalDateTime allocatedAt, LocalDateTime releasedAt, Long allocatedBy) {
        this.allocationId = allocationId;
        this.storeOrderLineId = storeOrderLineId;
        this.inventoryLotId = inventoryLotId;
        this.allocatedQuantity = allocatedQuantity;
        this.pickedQuantity = pickedQuantity;
        this.status = status;
        this.allocatedAt = allocatedAt;
        this.releasedAt = releasedAt;
        this.allocatedBy = allocatedBy;
    }

    public static StockAllocationJpaEntity fromDomain(StockAllocation allocation) {
        return StockAllocationJpaEntity.builder()
                .allocationId(allocation.getAllocationId())
                .storeOrderLineId(allocation.getStoreOrderLineId())
                .inventoryLotId(allocation.getInventoryLotId())
                .allocatedQuantity(allocation.getAllocatedQuantity())
                .pickedQuantity(allocation.getPickedQuantity())
                .status(allocation.getStatus())
                .allocatedAt(allocation.getAllocatedAt())
                .releasedAt(allocation.getReleasedAt())
                .allocatedBy(allocation.getAllocatedBy())
                .build();
    }

    public StockAllocation toDomain() {
        return StockAllocation.builder()
                .allocationId(allocationId)
                .storeOrderLineId(storeOrderLineId)
                .inventoryLotId(inventoryLotId)
                .allocatedQuantity(allocatedQuantity)
                .pickedQuantity(pickedQuantity)
                .status(status)
                .allocatedAt(allocatedAt)
                .releasedAt(releasedAt)
                .allocatedBy(allocatedBy)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

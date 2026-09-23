package com.kb.wms.inventory.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.enums.QualityStatus;

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
@Table(name = "inventory_lot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryLotJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_lot_id")
    private Long inventoryLotId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "on_hand_quantity", nullable = false)
    private Long onHandQuantity;

    @Column(name = "allocated_quantity", nullable = false)
    private Long allocatedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_status", nullable = false, length = 20)
    private QualityStatus qualityStatus;

    @Column(name = "last_counted_at")
    private LocalDateTime lastCountedAt;

    @Builder
    private InventoryLotJpaEntity(Long inventoryLotId, Long sectionId, Long lotId, Long onHandQuantity,
                                  Long allocatedQuantity, QualityStatus qualityStatus, LocalDateTime lastCountedAt) {
        this.inventoryLotId = inventoryLotId;
        this.sectionId = sectionId;
        this.lotId = lotId;
        this.onHandQuantity = onHandQuantity;
        this.allocatedQuantity = allocatedQuantity;
        this.qualityStatus = qualityStatus;
        this.lastCountedAt = lastCountedAt;
    }

    public static InventoryLotJpaEntity fromDomain(InventoryLot inventoryLot) {
        return InventoryLotJpaEntity.builder()
                .inventoryLotId(inventoryLot.getInventoryLotId())
                .sectionId(inventoryLot.getSectionId())
                .lotId(inventoryLot.getLotId())
                .onHandQuantity(inventoryLot.getOnHandQuantity())
                .allocatedQuantity(inventoryLot.getAllocatedQuantity())
                .qualityStatus(inventoryLot.getQualityStatus())
                .lastCountedAt(inventoryLot.getLastCountedAt())
                .build();
    }

    public InventoryLot toDomain() {
        return InventoryLot.builder()
                .inventoryLotId(inventoryLotId)
                .sectionId(sectionId)
                .lotId(lotId)
                .onHandQuantity(onHandQuantity)
                .allocatedQuantity(allocatedQuantity)
                .qualityStatus(qualityStatus)
                .lastCountedAt(lastCountedAt)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

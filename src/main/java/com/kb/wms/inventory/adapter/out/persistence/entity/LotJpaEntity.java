package com.kb.wms.inventory.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.domain.enums.LotStatus;

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
@Table(name = "lot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LotJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lot_id")
    private Long lotId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "lot_number", nullable = false, length = 100)
    private String lotNumber;

    @Column(name = "manufactured_date")
    private LocalDate manufacturedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LotStatus status;

    @Column(name = "unit_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitCost;

    @Builder
    private LotJpaEntity(Long lotId, Long skuId, Long supplierId, String lotNumber, LocalDate manufacturedDate,
                         LocalDate expiryDate, LotStatus status, BigDecimal unitCost) {
        this.lotId = lotId;
        this.skuId = skuId;
        this.supplierId = supplierId;
        this.lotNumber = lotNumber;
        this.manufacturedDate = manufacturedDate;
        this.expiryDate = expiryDate;
        this.status = status;
        this.unitCost = unitCost;
    }

    public static LotJpaEntity fromDomain(Lot lot) {
        return LotJpaEntity.builder()
                .lotId(lot.getLotId())
                .skuId(lot.getSkuId())
                .supplierId(lot.getSupplierId())
                .lotNumber(lot.getLotNumber())
                .manufacturedDate(lot.getManufacturedDate())
                .expiryDate(lot.getExpiryDate())
                .status(lot.getStatus())
                .unitCost(lot.getUnitCost())
                .build();
    }

    public Lot toDomain() {
        return Lot.builder()
                .lotId(lotId)
                .skuId(skuId)
                .supplierId(supplierId)
                .lotNumber(lotNumber)
                .manufacturedDate(manufacturedDate)
                .expiryDate(expiryDate)
                .status(status)
                .unitCost(unitCost)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

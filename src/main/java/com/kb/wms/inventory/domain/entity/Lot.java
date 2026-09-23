package com.kb.wms.inventory.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kb.wms.inventory.domain.enums.LotStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 동일 제조·입고 묶음을 식별하는 추적 단위(로트) 마스터다.
 * 같은 로트는 동일 원가(unit_cost)를 가지며, 입고 시점에 확정된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lot {

    private Long lotId;
    private Long skuId;
    private Long supplierId;
    private String lotNumber;
    private LocalDate manufacturedDate;
    private LocalDate expiryDate;
    private LotStatus status;
    private BigDecimal unitCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Lot(Long lotId, Long skuId, Long supplierId, String lotNumber, LocalDate manufacturedDate,
                LocalDate expiryDate, LotStatus status, BigDecimal unitCost,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.lotId = lotId;
        this.skuId = skuId;
        this.supplierId = supplierId;
        this.lotNumber = lotNumber;
        this.manufacturedDate = manufacturedDate;
        this.expiryDate = expiryDate;
        this.status = status == null ? LotStatus.AVAILABLE : status;
        this.unitCost = unitCost;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Lot register(Long skuId, Long supplierId, String lotNumber, LocalDate manufacturedDate,
                               LocalDate expiryDate, BigDecimal unitCost) {
        return Lot.builder()
                .skuId(skuId)
                .supplierId(supplierId)
                .lotNumber(lotNumber)
                .manufacturedDate(manufacturedDate)
                .expiryDate(expiryDate)
                .unitCost(unitCost)
                .status(LotStatus.AVAILABLE)
                .build();
    }

    public boolean isAvailable() {
        return this.status == LotStatus.AVAILABLE;
    }

    /**
     * 기준일에 유통기한이 지났는지 여부. 유통기한이 없는 로트는 만료되지 않는다.
     */
    public boolean isExpiredAt(LocalDate baseDate) {
        return this.expiryDate != null && this.expiryDate.isBefore(baseDate);
    }

    public void expire() {
        this.status = LotStatus.EXPIRED;
    }

    public void quarantine() {
        this.status = LotStatus.QUARANTINED;
    }

    public void dispose() {
        this.status = LotStatus.DISPOSED;
    }

    public void release() {
        this.status = LotStatus.AVAILABLE;
    }
}

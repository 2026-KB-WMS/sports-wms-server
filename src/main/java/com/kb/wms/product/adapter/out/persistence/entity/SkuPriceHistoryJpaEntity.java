package com.kb.wms.product.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.product.domain.entity.SkuPriceHistory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sku_price_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkuPriceHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_price_history_id")
    private Long skuPriceHistoryId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "previous_purchase_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal previousPurchasePrice;

    @Column(name = "new_purchase_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal newPurchasePrice;

    @Column(name = "previous_supply_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal previousSupplyPrice;

    @Column(name = "new_supply_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal newSupplyPrice;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Builder
    private SkuPriceHistoryJpaEntity(Long skuPriceHistoryId, Long skuId,
                                     BigDecimal previousPurchasePrice, BigDecimal newPurchasePrice,
                                     BigDecimal previousSupplyPrice, BigDecimal newSupplyPrice,
                                     Long changedBy, LocalDateTime changedAt) {
        this.skuPriceHistoryId = skuPriceHistoryId;
        this.skuId = skuId;
        this.previousPurchasePrice = previousPurchasePrice;
        this.newPurchasePrice = newPurchasePrice;
        this.previousSupplyPrice = previousSupplyPrice;
        this.newSupplyPrice = newSupplyPrice;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public static SkuPriceHistoryJpaEntity fromDomain(SkuPriceHistory history) {
        return SkuPriceHistoryJpaEntity.builder()
                .skuPriceHistoryId(history.getSkuPriceHistoryId())
                .skuId(history.getSkuId())
                .previousPurchasePrice(history.getPreviousPurchasePrice())
                .newPurchasePrice(history.getNewPurchasePrice())
                .previousSupplyPrice(history.getPreviousSupplyPrice())
                .newSupplyPrice(history.getNewSupplyPrice())
                .changedBy(history.getChangedBy())
                .changedAt(history.getChangedAt())
                .build();
    }

    public SkuPriceHistory toDomain() {
        return SkuPriceHistory.builder()
                .skuPriceHistoryId(skuPriceHistoryId)
                .skuId(skuId)
                .previousPurchasePrice(previousPurchasePrice)
                .newPurchasePrice(newPurchasePrice)
                .previousSupplyPrice(previousSupplyPrice)
                .newSupplyPrice(newSupplyPrice)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
    }
}

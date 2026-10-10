package com.kb.wms.product.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SKU 현재 단가(매입·공급)가 바뀐 기록. 단가가 바뀐 수정 한 번당 한 건이며, 바꾸지 않은 쪽 단가도 이전·이후가 같은 값으로 들어간다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkuPriceHistory {

    private Long skuPriceHistoryId;
    private Long skuId;
    private BigDecimal previousPurchasePrice;
    private BigDecimal newPurchasePrice;
    private BigDecimal previousSupplyPrice;
    private BigDecimal newSupplyPrice;
    private Long changedBy;
    private LocalDateTime changedAt;

    @Builder
    private SkuPriceHistory(Long skuPriceHistoryId, Long skuId,
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

    public static SkuPriceHistory record(Long skuId,
                                         BigDecimal previousPurchasePrice, BigDecimal newPurchasePrice,
                                         BigDecimal previousSupplyPrice, BigDecimal newSupplyPrice,
                                         Long changedBy) {
        if (skuId == null || changedBy == null) {
            throw new IllegalArgumentException("SKU와 처리자는 필수입니다.");
        }
        return SkuPriceHistory.builder()
                .skuId(skuId)
                .previousPurchasePrice(previousPurchasePrice)
                .newPurchasePrice(newPurchasePrice)
                .previousSupplyPrice(previousSupplyPrice)
                .newSupplyPrice(newSupplyPrice)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
    }
}

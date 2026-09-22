package com.kb.wms.product.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.product.domain.enums.ProductStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 옵션까지 확정된 실제 재고·입출고 관리 단위(SKU).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSku {

    private static final String DEFAULT_UNIT = "EA";

    private Long skuId;
    private Long productId;
    private String skuCode;
    private String barcode;
    private String name;
    private BigDecimal weight;
    private BigDecimal currentPurchasePrice;
    private BigDecimal currentSupplyPrice;
    private String unit;
    private BigDecimal safetyStockQuantity;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private ProductSku(Long skuId, Long productId, String skuCode, String barcode, String name,
                        BigDecimal weight, BigDecimal currentPurchasePrice, BigDecimal currentSupplyPrice,
                        String unit, BigDecimal safetyStockQuantity, ProductStatus status,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.skuId = skuId;
        this.productId = productId;
        this.skuCode = skuCode;
        this.barcode = barcode;
        this.name = name;
        this.weight = weight;
        this.currentPurchasePrice = currentPurchasePrice;
        this.currentSupplyPrice = currentSupplyPrice;
        this.unit = unit == null ? DEFAULT_UNIT : unit;
        this.safetyStockQuantity = safetyStockQuantity == null ? BigDecimal.ZERO : safetyStockQuantity;
        this.status = status == null ? ProductStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductSku register(Long productId, String skuCode, String barcode, String name,
                                        BigDecimal weight, BigDecimal currentPurchasePrice,
                                        BigDecimal currentSupplyPrice, String unit,
                                        BigDecimal safetyStockQuantity) {
        return ProductSku.builder()
                .productId(productId)
                .skuCode(skuCode)
                .barcode(barcode)
                .name(name)
                .weight(weight)
                .currentPurchasePrice(currentPurchasePrice)
                .currentSupplyPrice(currentSupplyPrice)
                .unit(unit)
                .safetyStockQuantity(safetyStockQuantity)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    /**
     * 점주(STORE_OWNER) 응답에서는 매입 단가·안전 재고를 노출하지 않는다.
     */
    public boolean isPurchaseInfoVisibleTo(boolean isStoreOwner) {
        return !isStoreOwner;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == ProductStatus.ACTIVE;
    }
}

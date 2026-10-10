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
    private Long safetyStockQuantity;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private ProductSku(Long skuId, Long productId, String skuCode, String barcode, String name,
                        BigDecimal weight, BigDecimal currentPurchasePrice, BigDecimal currentSupplyPrice,
                        String unit, Long safetyStockQuantity, ProductStatus status,
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
        this.safetyStockQuantity = safetyStockQuantity == null ? 0L : safetyStockQuantity;
        this.status = status == null ? ProductStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductSku register(Long productId, String skuCode, String barcode, String name,
                                        BigDecimal weight, BigDecimal currentPurchasePrice,
                                        BigDecimal currentSupplyPrice, String unit,
                                        Long safetyStockQuantity) {
        requirePrice(currentPurchasePrice, "매입 단가");
        requirePrice(currentSupplyPrice, "공급 단가");
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
     * 값이 있는 필드만 바꾼다. 단가는 0 이상이어야 한다. skuCode·productId·unit·상태는 여기서 바꾸지 않는다.
     */
    public void update(String name, String barcode, BigDecimal weight, BigDecimal currentPurchasePrice,
                       BigDecimal currentSupplyPrice, Long safetyStockQuantity) {
        if (name != null) {
            this.name = name;
        }
        if (barcode != null) {
            this.barcode = barcode;
        }
        if (weight != null) {
            this.weight = weight;
        }
        if (currentPurchasePrice != null) {
            requirePrice(currentPurchasePrice, "매입 단가");
            this.currentPurchasePrice = currentPurchasePrice;
        }
        if (currentSupplyPrice != null) {
            requirePrice(currentSupplyPrice, "공급 단가");
            this.currentSupplyPrice = currentSupplyPrice;
        }
        if (safetyStockQuantity != null) {
            this.safetyStockQuantity = safetyStockQuantity;
        }
    }

    private static void requirePrice(BigDecimal price, String label) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException(label + "는 0 이상이어야 합니다.");
        }
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

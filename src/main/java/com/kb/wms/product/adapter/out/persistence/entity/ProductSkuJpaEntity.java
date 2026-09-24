package com.kb.wms.product.adapter.out.persistence.entity;

import java.math.BigDecimal;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.product.domain.enums.ProductStatus;

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
@Table(name = "product_sku",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_sku_code", columnNames = "sku_code"),
                @UniqueConstraint(name = "uk_product_sku_barcode", columnNames = "barcode")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSkuJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_id")
    private Long skuId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_code", nullable = false, length = 50)
    private String skuCode;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "weight", precision = 12, scale = 3)
    private BigDecimal weight;

    @Column(name = "current_purchase_price", precision = 18, scale = 2)
    private BigDecimal currentPurchasePrice;

    @Column(name = "current_supply_price", precision = 18, scale = 2)
    private BigDecimal currentSupplyPrice;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "safety_stock_quantity", nullable = false)
    private Long safetyStockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Builder
    private ProductSkuJpaEntity(Long skuId, Long productId, String skuCode, String barcode, String name,
                                 BigDecimal weight, BigDecimal currentPurchasePrice, BigDecimal currentSupplyPrice,
                                 String unit, Long safetyStockQuantity, ProductStatus status) {
        this.skuId = skuId;
        this.productId = productId;
        this.skuCode = skuCode;
        this.barcode = barcode;
        this.name = name;
        this.weight = weight;
        this.currentPurchasePrice = currentPurchasePrice;
        this.currentSupplyPrice = currentSupplyPrice;
        this.unit = unit;
        this.safetyStockQuantity = safetyStockQuantity;
        this.status = status;
    }

    public static ProductSkuJpaEntity fromDomain(ProductSku productSku) {
        return ProductSkuJpaEntity.builder()
                .skuId(productSku.getSkuId())
                .productId(productSku.getProductId())
                .skuCode(productSku.getSkuCode())
                .barcode(productSku.getBarcode())
                .name(productSku.getName())
                .weight(productSku.getWeight())
                .currentPurchasePrice(productSku.getCurrentPurchasePrice())
                .currentSupplyPrice(productSku.getCurrentSupplyPrice())
                .unit(productSku.getUnit())
                .safetyStockQuantity(productSku.getSafetyStockQuantity())
                .status(productSku.getStatus())
                .build();
    }

    public ProductSku toDomain() {
        return ProductSku.builder()
                .skuId(skuId)
                .productId(productId)
                .skuCode(skuCode)
                .barcode(barcode)
                .name(name)
                .weight(weight)
                .currentPurchasePrice(currentPurchasePrice)
                .currentSupplyPrice(currentSupplyPrice)
                .unit(unit)
                .safetyStockQuantity(safetyStockQuantity)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

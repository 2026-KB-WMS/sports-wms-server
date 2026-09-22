package com.kb.wms.product.adapter.out.persistence.entity;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.domain.enums.ProductStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Builder
    private ProductJpaEntity(Long productId, Long brandId, Long categoryId, String productCode, String name,
                              String description, ProductStatus status) {
        this.productId = productId;
        this.brandId = brandId;
        this.categoryId = categoryId;
        this.productCode = productCode;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public static ProductJpaEntity fromDomain(Product product) {
        return ProductJpaEntity.builder()
                .productId(product.getProductId())
                .brandId(product.getBrandId())
                .categoryId(product.getCategoryId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .description(product.getDescription())
                .status(product.getStatus())
                .build();
    }

    public Product toDomain() {
        return Product.builder()
                .productId(productId)
                .brandId(brandId)
                .categoryId(categoryId)
                .productCode(productCode)
                .name(name)
                .description(description)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

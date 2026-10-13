package com.kb.wms.product.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.kb.wms.product.domain.entity.ProductSpec;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "product_spec",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_spec",
                columnNames = {"product_id", "option_value_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSpecJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_spec_id")
    private Long productSpecId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "option_value_id", nullable = false)
    private Long optionValueId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private ProductSpecJpaEntity(Long productSpecId, Long productId, Long optionValueId) {
        this.productSpecId = productSpecId;
        this.productId = productId;
        this.optionValueId = optionValueId;
    }

    public static ProductSpecJpaEntity fromDomain(ProductSpec domain) {
        return ProductSpecJpaEntity.builder()
                .productSpecId(domain.getProductSpecId())
                .productId(domain.getProductId())
                .optionValueId(domain.getOptionValueId())
                .build();
    }

    public ProductSpec toDomain() {
        return ProductSpec.builder()
                .productSpecId(productSpecId)
                .productId(productId)
                .optionValueId(optionValueId)
                .createdAt(createdAt)
                .build();
    }
}

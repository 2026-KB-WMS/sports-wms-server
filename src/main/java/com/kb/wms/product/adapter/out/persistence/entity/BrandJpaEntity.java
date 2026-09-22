package com.kb.wms.product.adapter.out.persistence.entity;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.enums.ProductStatus;

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
@Table(name = "brand")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Builder
    private BrandJpaEntity(Long brandId, String name, String description, ProductStatus status) {
        this.brandId = brandId;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public static BrandJpaEntity fromDomain(Brand brand) {
        return BrandJpaEntity.builder()
                .brandId(brand.getBrandId())
                .name(brand.getName())
                .description(brand.getDescription())
                .status(brand.getStatus())
                .build();
    }

    public Brand toDomain() {
        return Brand.builder()
                .brandId(brandId)
                .name(name)
                .description(description)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

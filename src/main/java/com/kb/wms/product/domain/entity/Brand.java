package com.kb.wms.product.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.product.domain.enums.ProductStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 브랜드 마스터.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand {

    private Long brandId;
    private String name;
    private String description;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Brand(Long brandId, String name, String description, ProductStatus status,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.brandId = brandId;
        this.name = name;
        this.description = description;
        this.status = status == null ? ProductStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Brand register(String name, String description) {
        return Brand.builder()
                .name(name)
                .description(description)
                .status(ProductStatus.ACTIVE)
                .build();
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

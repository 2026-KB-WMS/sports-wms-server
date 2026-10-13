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
    private String brandCode;
    private String name;
    private String description;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Brand(Long brandId, String brandCode, String name, String description, ProductStatus status,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.brandId = brandId;
        this.brandCode = brandCode;
        this.name = name;
        this.description = description;
        this.status = status == null ? ProductStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 코드가 없는 형태. 코드 도입 전 브랜드와 같다. */
    public static Brand register(String name, String description) {
        return register(null, name, description);
    }

    public static Brand register(String brandCode, String name, String description) {
        return Brand.builder()
                .brandCode(brandCode)
                .name(name)
                .description(description)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    /** 코드가 없는 기존 브랜드에 한 번만 지정한다. 코드가 이미 있으면 바꿀 수 없다. */
    public void assignBrandCode(String brandCode) {
        if (this.brandCode != null) {
            throw new IllegalStateException("브랜드 코드는 한 번 지정하면 바꿀 수 없습니다.");
        }
        this.brandCode = brandCode;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeDescription(String description) {
        this.description = description;
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

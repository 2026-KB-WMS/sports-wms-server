package com.kb.wms.product.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.product.domain.enums.ProductStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브랜드·카테고리 단위의 상품 공통 정보.
 * 옵션 조합까지 확정된 재고·입출고 관리 단위는 {@link ProductSku}가 별도로 관리한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    private Long productId;
    private Long brandId;
    private Long categoryId;
    private String productCode;
    private String name;
    private String description;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Product(Long productId, Long brandId, Long categoryId, String productCode, String name,
                     String description, ProductStatus status,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.productId = productId;
        this.brandId = brandId;
        this.categoryId = categoryId;
        this.productCode = productCode;
        this.name = name;
        this.description = description;
        this.status = status == null ? ProductStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product register(Long brandId, Long categoryId, String productCode,
                                    String name, String description) {
        return Product.builder()
                .brandId(brandId)
                .categoryId(categoryId)
                .productCode(productCode)
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

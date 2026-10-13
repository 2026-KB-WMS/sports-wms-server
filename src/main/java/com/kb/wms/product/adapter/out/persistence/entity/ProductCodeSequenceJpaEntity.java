package com.kb.wms.product.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브랜드+카테고리 조합별 마지막 발급 번호. 코드가 재발급되어도 번호가 되돌아가지 않도록
 * 기존 상품 개수가 아니라 이 값으로 채번한다.
 */
@Entity
@Table(name = "product_code_sequence")
@IdClass(ProductCodeSequenceJpaEntity.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCodeSequenceJpaEntity {

    @Id
    @Column(name = "brand_id")
    private Long brandId;

    @Id
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "last_no", nullable = false)
    private long lastNo;

    public ProductCodeSequenceJpaEntity(Long brandId, Long categoryId) {
        this.brandId = brandId;
        this.categoryId = categoryId;
        this.lastNo = 0;
    }

    public long next() {
        this.lastNo++;
        return this.lastNo;
    }

    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Key implements java.io.Serializable {
        private Long brandId;
        private Long categoryId;
    }
}

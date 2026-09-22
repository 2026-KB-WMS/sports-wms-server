package com.kb.wms.product.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import com.kb.wms.product.domain.entity.SkuOptionValue;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "sku_option_value")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkuOptionValueJpaEntity {

    @EmbeddedId
    private SkuOptionValueId id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private SkuOptionValueJpaEntity(SkuOptionValueId id) {
        this.id = id;
    }

    public static SkuOptionValueJpaEntity fromDomain(SkuOptionValue skuOptionValue) {
        return SkuOptionValueJpaEntity.builder()
                .id(new SkuOptionValueId(skuOptionValue.getSkuId(), skuOptionValue.getOptionValueId()))
                .build();
    }

    public SkuOptionValue toDomain() {
        return SkuOptionValue.builder()
                .skuId(id.getSkuId())
                .optionValueId(id.getOptionValueId())
                .createdAt(createdAt)
                .build();
    }
}

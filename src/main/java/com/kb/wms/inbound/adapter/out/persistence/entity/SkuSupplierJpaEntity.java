package com.kb.wms.inbound.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.kb.wms.inbound.domain.entity.SkuSupplier;

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
@Table(name = "sku_supplier",
        uniqueConstraints = @UniqueConstraint(name = "uk_sku_supplier_sku_supplier",
                columnNames = {"sku_id", "supplier_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkuSupplierJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_supplier_id")
    private Long skuSupplierId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private SkuSupplierJpaEntity(Long skuSupplierId, Long skuId, Long supplierId) {
        this.skuSupplierId = skuSupplierId;
        this.skuId = skuId;
        this.supplierId = supplierId;
    }

    public static SkuSupplierJpaEntity fromDomain(SkuSupplier skuSupplier) {
        return SkuSupplierJpaEntity.builder()
                .skuSupplierId(skuSupplier.getSkuSupplierId())
                .skuId(skuSupplier.getSkuId())
                .supplierId(skuSupplier.getSupplierId())
                .build();
    }

    public SkuSupplier toDomain() {
        return SkuSupplier.builder()
                .skuSupplierId(skuSupplierId)
                .skuId(skuId)
                .supplierId(supplierId)
                .createdAt(createdAt)
                .build();
    }
}

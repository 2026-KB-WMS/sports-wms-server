package com.kb.wms.product.adapter.out.persistence.entity;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.product.domain.entity.OptionValue;
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
@Table(name = "option_value")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionValueJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_value_id")
    private Long optionValueId;

    @Column(name = "option_group_id", nullable = false)
    private Long optionGroupId;

    @Column(name = "value", nullable = false, length = 100)
    private String value;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Builder
    private OptionValueJpaEntity(Long optionValueId, Long optionGroupId, String value, int sortOrder,
                                  ProductStatus status) {
        this.optionValueId = optionValueId;
        this.optionGroupId = optionGroupId;
        this.value = value;
        this.sortOrder = sortOrder;
        this.status = status;
    }

    public static OptionValueJpaEntity fromDomain(OptionValue optionValue) {
        return OptionValueJpaEntity.builder()
                .optionValueId(optionValue.getOptionValueId())
                .optionGroupId(optionValue.getOptionGroupId())
                .value(optionValue.getValue())
                .sortOrder(optionValue.getSortOrder())
                .status(optionValue.getStatus())
                .build();
    }

    public OptionValue toDomain() {
        return OptionValue.builder()
                .optionValueId(optionValueId)
                .optionGroupId(optionGroupId)
                .value(value)
                .sortOrder(sortOrder)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

package com.kb.wms.product.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.product.domain.enums.ProductStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 옵션 그룹에 속하는 구체적 옵션 값(예: 빨강, L).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionValue {

    private Long optionValueId;
    private Long optionGroupId;
    private String value;
    private int sortOrder;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private OptionValue(Long optionValueId, Long optionGroupId, String value, int sortOrder,
                         ProductStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.optionValueId = optionValueId;
        this.optionGroupId = optionGroupId;
        this.value = value;
        this.sortOrder = sortOrder;
        this.status = status == null ? ProductStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OptionValue register(Long optionGroupId, String value, int sortOrder) {
        return OptionValue.builder()
                .optionGroupId(optionGroupId)
                .value(value)
                .sortOrder(sortOrder)
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

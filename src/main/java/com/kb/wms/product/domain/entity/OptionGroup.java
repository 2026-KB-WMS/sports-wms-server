package com.kb.wms.product.domain.entity;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 색상·사이즈처럼 SKU를 구성하는 옵션 그룹.
 * 특정 상품에 종속되지 않는 공용 마스터다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionGroup {

    private Long optionGroupId;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private OptionGroup(Long optionGroupId, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.optionGroupId = optionGroupId;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OptionGroup register(String name) {
        return OptionGroup.builder()
                .name(name)
                .build();
    }
}

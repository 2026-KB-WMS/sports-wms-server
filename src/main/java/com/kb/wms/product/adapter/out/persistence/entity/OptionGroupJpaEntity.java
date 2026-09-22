package com.kb.wms.product.adapter.out.persistence.entity;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.product.domain.entity.OptionGroup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "option_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionGroupJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_group_id")
    private Long optionGroupId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Builder
    private OptionGroupJpaEntity(Long optionGroupId, String name) {
        this.optionGroupId = optionGroupId;
        this.name = name;
    }

    public static OptionGroupJpaEntity fromDomain(OptionGroup optionGroup) {
        return OptionGroupJpaEntity.builder()
                .optionGroupId(optionGroup.getOptionGroupId())
                .name(optionGroup.getName())
                .build();
    }

    public OptionGroup toDomain() {
        return OptionGroup.builder()
                .optionGroupId(optionGroupId)
                .name(name)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

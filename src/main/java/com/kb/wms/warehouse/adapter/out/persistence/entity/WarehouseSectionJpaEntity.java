package com.kb.wms.warehouse.adapter.out.persistence.entity;

import java.math.BigDecimal;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "warehouse_section",
        uniqueConstraints = @UniqueConstraint(name = "uk_warehouse_section_code",
                columnNames = {"warehouse_id", "section_code"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseSectionJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "parent_section_id")
    private Long parentSectionId;

    @Column(name = "section_code", nullable = false, length = 50)
    private String sectionCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "section_type", nullable = false, length = 20)
    private String sectionType;

    @Column(name = "capacity", nullable = false, precision = 14, scale = 3)
    private BigDecimal capacity;

    @Column(name = "current_capacity", nullable = false, precision = 14, scale = 3)
    private BigDecimal currentCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WarehouseStatus status;

    @Builder
    private WarehouseSectionJpaEntity(Long sectionId, Long warehouseId, Long parentSectionId, String sectionCode,
                                       String name, String sectionType, BigDecimal capacity,
                                       BigDecimal currentCapacity, WarehouseStatus status) {
        this.sectionId = sectionId;
        this.warehouseId = warehouseId;
        this.parentSectionId = parentSectionId;
        this.sectionCode = sectionCode;
        this.name = name;
        this.sectionType = sectionType;
        this.capacity = capacity;
        this.currentCapacity = currentCapacity;
        this.status = status;
    }

    public static WarehouseSectionJpaEntity fromDomain(WarehouseSection section) {
        return WarehouseSectionJpaEntity.builder()
                .sectionId(section.getSectionId())
                .warehouseId(section.getWarehouseId())
                .parentSectionId(section.getParentSectionId())
                .sectionCode(section.getSectionCode())
                .name(section.getName())
                .sectionType(section.getSectionType())
                .capacity(section.getCapacity())
                .currentCapacity(section.getCurrentCapacity())
                .status(section.getStatus())
                .build();
    }

    public WarehouseSection toDomain() {
        return WarehouseSection.builder()
                .sectionId(sectionId)
                .warehouseId(warehouseId)
                .parentSectionId(parentSectionId)
                .sectionCode(sectionCode)
                .name(name)
                .sectionType(sectionType)
                .capacity(capacity)
                .currentCapacity(currentCapacity)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

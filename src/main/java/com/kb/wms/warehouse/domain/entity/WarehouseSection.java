package com.kb.wms.warehouse.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 창고 내부의 실제 보관 구역(구역·랙 등)을 계층적으로 관리한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseSection {

    private Long sectionId;
    private Long warehouseId;
    private Long parentSectionId;
    private String sectionCode;
    private String name;
    private String sectionType;
    private BigDecimal capacity;
    private BigDecimal currentCapacity;
    private WarehouseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private WarehouseSection(Long sectionId, Long warehouseId, Long parentSectionId, String sectionCode,
                              String name, String sectionType, BigDecimal capacity, BigDecimal currentCapacity,
                              WarehouseStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.sectionId = sectionId;
        this.warehouseId = warehouseId;
        this.parentSectionId = parentSectionId;
        this.sectionCode = sectionCode;
        this.name = name;
        this.sectionType = sectionType;
        this.capacity = capacity == null ? BigDecimal.ZERO : capacity;
        this.currentCapacity = currentCapacity == null ? BigDecimal.ZERO : currentCapacity;
        this.status = status == null ? WarehouseStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WarehouseSection register(Long warehouseId, Long parentSectionId, String sectionCode,
                                              String name, String sectionType, BigDecimal capacity) {
        return WarehouseSection.builder()
                .warehouseId(warehouseId)
                .parentSectionId(parentSectionId)
                .sectionCode(sectionCode)
                .name(name)
                .sectionType(sectionType)
                .capacity(capacity)
                .status(WarehouseStatus.ACTIVE)
                .build();
    }

    public boolean isRoot() {
        return this.parentSectionId == null;
    }

    public BigDecimal availableCapacity() {
        return this.capacity.subtract(this.currentCapacity);
    }

    /** 수량만큼 적치해도 수용량을 넘지 않는지 */
    public boolean canOccupy(BigDecimal quantity) {
        return availableCapacity().compareTo(quantity) >= 0;
    }

    /** 재고 적치로 사용 용량을 늘린다. 수용량 검증은 호출 전에 {@link #canOccupy}로 한다. */
    public void occupy(BigDecimal quantity) {
        this.currentCapacity = this.currentCapacity.add(quantity);
    }

    /** 재고 반출로 사용 용량을 줄인다. 0 아래로는 내려가지 않는다. */
    public void vacate(BigDecimal quantity) {
        this.currentCapacity = this.currentCapacity.subtract(quantity).max(BigDecimal.ZERO);
    }

    public void changeSectionCode(String sectionCode) {
        this.sectionCode = sectionCode;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeSectionType(String sectionType) {
        this.sectionType = sectionType;
    }

    public void changeCapacity(BigDecimal capacity) {
        this.capacity = capacity;
    }

    public void deactivate() {
        this.status = WarehouseStatus.INACTIVE;
    }

    public void activate() {
        this.status = WarehouseStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == WarehouseStatus.ACTIVE;
    }
}

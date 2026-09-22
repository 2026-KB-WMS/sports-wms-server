package com.kb.wms.warehouse.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고를 보관하고 입고·출고를 수행하는 물류 거점 마스터.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Warehouse {

    private Long warehouseId;
    private String warehouseCode;
    private String name;
    private String address;
    private String contactNumber;
    private BigDecimal totalCapacity;
    private WarehouseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Warehouse(Long warehouseId, String warehouseCode, String name, String address,
                       String contactNumber, BigDecimal totalCapacity, WarehouseStatus status,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.name = name;
        this.address = address;
        this.contactNumber = contactNumber;
        this.totalCapacity = totalCapacity == null ? BigDecimal.ZERO : totalCapacity;
        this.status = status == null ? WarehouseStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Warehouse register(String warehouseCode, String name, String address,
                                      String contactNumber, BigDecimal totalCapacity) {
        return Warehouse.builder()
                .warehouseCode(warehouseCode)
                .name(name)
                .address(address)
                .contactNumber(contactNumber)
                .totalCapacity(totalCapacity)
                .status(WarehouseStatus.ACTIVE)
                .build();
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeAddress(String address) {
        this.address = address;
    }

    public void changeContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void changeTotalCapacity(BigDecimal totalCapacity) {
        this.totalCapacity = totalCapacity;
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

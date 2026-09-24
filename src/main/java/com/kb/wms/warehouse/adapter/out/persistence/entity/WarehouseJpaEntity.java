package com.kb.wms.warehouse.adapter.out.persistence.entity;

import java.math.BigDecimal;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.warehouse.domain.entity.Warehouse;
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
@Table(name = "warehouse",
        uniqueConstraints = @UniqueConstraint(name = "uk_warehouse_code", columnNames = "warehouse_code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_code", nullable = false, length = 30)
    private String warehouseCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Column(name = "total_capacity", nullable = false, precision = 14, scale = 3)
    private BigDecimal totalCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WarehouseStatus status;

    @Builder
    private WarehouseJpaEntity(Long warehouseId, String warehouseCode, String name, String address,
                                String contactNumber, BigDecimal totalCapacity, WarehouseStatus status) {
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.name = name;
        this.address = address;
        this.contactNumber = contactNumber;
        this.totalCapacity = totalCapacity;
        this.status = status;
    }

    public static WarehouseJpaEntity fromDomain(Warehouse warehouse) {
        return WarehouseJpaEntity.builder()
                .warehouseId(warehouse.getWarehouseId())
                .warehouseCode(warehouse.getWarehouseCode())
                .name(warehouse.getName())
                .address(warehouse.getAddress())
                .contactNumber(warehouse.getContactNumber())
                .totalCapacity(warehouse.getTotalCapacity())
                .status(warehouse.getStatus())
                .build();
    }

    public Warehouse toDomain() {
        return Warehouse.builder()
                .warehouseId(warehouseId)
                .warehouseCode(warehouseCode)
                .name(name)
                .address(address)
                .contactNumber(contactNumber)
                .totalCapacity(totalCapacity)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

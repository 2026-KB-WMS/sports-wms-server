package com.kb.wms.inbound.adapter.out.persistence.entity;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.enums.SupplierStatus;

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
@Table(name = "supplier",
        uniqueConstraints = @UniqueConstraint(name = "uk_supplier_code", columnNames = "supplier_code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_code", nullable = false, length = 30)
    private String supplierCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "manager_name", nullable = false, length = 100)
    private String managerName;

    @Column(name = "contact_number", nullable = false, length = 30)
    private String contactNumber;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupplierStatus status;

    @Builder
    private SupplierJpaEntity(Long supplierId, String supplierCode, String name, String managerName,
                              String contactNumber, String email, String address, SupplierStatus status) {
        this.supplierId = supplierId;
        this.supplierCode = supplierCode;
        this.name = name;
        this.managerName = managerName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.address = address;
        this.status = status;
    }

    public static SupplierJpaEntity fromDomain(Supplier supplier) {
        return SupplierJpaEntity.builder()
                .supplierId(supplier.getSupplierId())
                .supplierCode(supplier.getSupplierCode())
                .name(supplier.getName())
                .managerName(supplier.getManagerName())
                .contactNumber(supplier.getContactNumber())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .status(supplier.getStatus())
                .build();
    }

    public Supplier toDomain() {
        return Supplier.builder()
                .supplierId(supplierId)
                .supplierCode(supplierCode)
                .name(name)
                .managerName(managerName)
                .contactNumber(contactNumber)
                .email(email)
                .address(address)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

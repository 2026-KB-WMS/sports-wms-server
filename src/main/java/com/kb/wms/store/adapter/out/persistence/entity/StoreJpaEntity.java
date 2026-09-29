package com.kb.wms.store.adapter.out.persistence.entity;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.domain.enums.StoreStatus;

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
@Table(name = "store",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_code", columnNames = "store_code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "store_code", nullable = false, length = 30)
    private String storeCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_number", nullable = false, length = 30)
    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreStatus status;

    @Builder
    private StoreJpaEntity(Long storeId, String storeCode, String name, String address, String contactName,
                           String contactNumber, StoreStatus status) {
        this.storeId = storeId;
        this.storeCode = storeCode;
        this.name = name;
        this.address = address;
        this.contactName = contactName;
        this.contactNumber = contactNumber;
        this.status = status;
    }

    public static StoreJpaEntity fromDomain(Store store) {
        return StoreJpaEntity.builder()
                .storeId(store.getStoreId())
                .storeCode(store.getStoreCode())
                .name(store.getName())
                .address(store.getAddress())
                .contactName(store.getContactName())
                .contactNumber(store.getContactNumber())
                .status(store.getStatus())
                .build();
    }

    public Store toDomain() {
        return Store.builder()
                .storeId(storeId)
                .storeCode(storeCode)
                .name(name)
                .address(address)
                .contactName(contactName)
                .contactNumber(contactNumber)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}

package com.kb.wms.inbound.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.inbound.domain.enums.SupplierStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 창고 발주(PurchaseOrder)의 대상이 되는 공급업체 마스터.
 * 삭제 없이 비활성화(ACTIVE → INACTIVE)만 허용한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Supplier {

    private Long supplierId;
    private String supplierCode;
    private String name;
    private String managerName;
    private String contactNumber;
    private String email;
    private String address;
    private SupplierStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Supplier(Long supplierId, String supplierCode, String name, String managerName,
                     String contactNumber, String email, String address, SupplierStatus status,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.supplierId = supplierId;
        this.supplierCode = supplierCode;
        this.name = name;
        this.managerName = managerName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.address = address;
        this.status = status == null ? SupplierStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Supplier register(String supplierCode, String name, String managerName,
                                    String contactNumber, String email, String address) {
        return Supplier.builder()
                .supplierCode(supplierCode)
                .name(name)
                .managerName(managerName)
                .contactNumber(contactNumber)
                .email(email)
                .address(address)
                .status(SupplierStatus.ACTIVE)
                .build();
    }

    // supplierCode는 등록 후 수정할 수 없으므로 변경 메서드를 두지 않는다.

    public void changeName(String name) {
        this.name = name;
    }

    public void changeManagerName(String managerName) {
        this.managerName = managerName;
    }

    public void changeContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    public void changeAddress(String address) {
        this.address = address;
    }

    /** 공급처 비활성화. 이미 비활성이면 예외 (서비스에서 먼저 검사해 409로 변환한다). */
    public void deactivate() {
        if (!isActive()) {
            throw new IllegalStateException("이미 비활성화된 공급처입니다.");
        }
        this.status = SupplierStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == SupplierStatus.ACTIVE;
    }
}

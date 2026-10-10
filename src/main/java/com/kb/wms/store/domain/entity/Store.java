package com.kb.wms.store.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.store.domain.enums.StoreStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품을 요청받는 판매 지점 마스터.
 * 재고를 갖지 않는 독립 도메인이며, 삭제 없이 비활성화(ACTIVE → INACTIVE)만 허용한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    private Long storeId;
    private String storeCode;
    private String name;
    private String address;
    private String contactName;
    private String contactNumber;
    private StoreStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Store(Long storeId, String storeCode, String name, String address, String contactName,
                  String contactNumber, StoreStatus status, LocalDateTime createdAt,
                  LocalDateTime updatedAt) {
        this.storeId = storeId;
        this.storeCode = storeCode;
        this.name = name;
        this.address = address;
        this.contactName = contactName;
        this.contactNumber = contactNumber;
        this.status = status == null ? StoreStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Store register(String storeCode, String name, String address, String contactName,
                                 String contactNumber) {
        return Store.builder()
                .storeCode(storeCode)
                .name(name)
                .address(address)
                .contactName(contactName)
                .contactNumber(contactNumber)
                .status(StoreStatus.ACTIVE)
                .build();
    }

    // storeCode는 등록 후 수정할 수 없으므로 변경 메서드를 두지 않는다.

    public void changeName(String name) {
        this.name = name;
    }

    public void changeAddress(String address) {
        this.address = address;
    }

    public void changeContactName(String contactName) {
        this.contactName = contactName;
    }

    public void changeContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    /** 지점 비활성화. 이미 비활성이면 예외 (서비스에서 먼저 검사해 409로 변환한다). */
    public void deactivate() {
        if (!isActive()) {
            throw new IllegalStateException("이미 비활성화된 지점입니다.");
        }
        this.status = StoreStatus.INACTIVE;
    }

    /** 지점 재활성화. 이미 활성이면 예외 (서비스에서 먼저 검사해 409로 변환한다). */
    public void activate() {
        if (isActive()) {
            throw new IllegalStateException("이미 활성화된 지점입니다.");
        }
        this.status = StoreStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == StoreStatus.ACTIVE;
    }
}

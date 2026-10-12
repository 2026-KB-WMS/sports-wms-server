package com.kb.wms.store.application.port.in.command;

import java.math.BigDecimal;

/**
 * PATCH /api/v1/stores/{storeId} 요청. 모든 필드는 선택이며, 값이 있는 필드만 부분 수정한다.
 * storeCode와 운영 상태(isActive)는 이 커맨드에 포함하지 않는다.
 */
public record StoreUpdateCommand(
        String name,
        String address,
        String contactName,
        String contactNumber,
        BigDecimal latitude,
        BigDecimal longitude
) {

    public StoreUpdateCommand(String name, String address, String contactName, String contactNumber) {
        this(name, address, contactName, contactNumber, null, null);
    }

    public boolean hasNoChanges() {
        return name == null && address == null && contactName == null && contactNumber == null
                && latitude == null && longitude == null;
    }
}

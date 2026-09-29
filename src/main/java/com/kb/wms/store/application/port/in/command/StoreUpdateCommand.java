package com.kb.wms.store.application.port.in.command;

/**
 * PATCH /api/v1/stores/{storeId} 요청. 모든 필드는 선택이며, 값이 있는 필드만 부분 수정한다.
 * storeCode와 운영 상태(isActive)는 이 커맨드에 포함하지 않는다.
 */
public record StoreUpdateCommand(
        String name,
        String address,
        String contactName,
        String contactNumber
) {

    public boolean hasNoChanges() {
        return name == null && address == null && contactName == null && contactNumber == null;
    }
}

package com.kb.wms.store.application.port.in.command;

/**
 * POST /api/v1/stores 요청. 점주 배정은 포함하지 않는다(POST /stores/assign으로 별도 처리).
 *
 * @param contactName 지점 담당자명 (선택)
 */
public record StoreRegisterCommand(
        String storeCode,
        String name,
        String address,
        String contactName,
        String contactNumber
) {
}

package com.kb.wms.store.application.port.in.command;

import java.math.BigDecimal;

/**
 * POST /api/v1/stores 요청. 점주 배정은 포함하지 않는다(POST /stores/assign으로 별도 처리).
 * 지점 코드는 받지 않고 주소의 시·도로 서버가 발급한다.
 *
 * @param contactName 지점 담당자명 (선택)
 */
public record StoreRegisterCommand(
        String name,
        String address,
        String contactName,
        String contactNumber,
        BigDecimal latitude,
        BigDecimal longitude
) {

    public StoreRegisterCommand(String name, String address, String contactName, String contactNumber) {
        this(name, address, contactName, contactNumber, null, null);
    }
}

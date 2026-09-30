package com.kb.wms.inbound.application.port.in.command;

/**
 * PATCH /api/v1/suppliers/{supplierId} 요청. 보낸 필드만 부분 수정한다.
 * supplierCode와 거래 상태(isActive)는 이 커맨드에 포함하지 않는다.
 *
 * <p>name·managerName·contactNumber는 null이면 "변경 없음"이다(비울 수 없는 필수 항목).
 * email·address는 명세상 null을 보내면 값을 비우므로, "변경 없음"과 "비움"을 구분하기 위해
 * clearEmail·clearAddress 플래그를 함께 받는다. 웹 어댑터가 JSON에 해당 키가 명시적 null로 왔을 때 true로 채운다.
 * 값(email/address)이 있으면 clear 플래그보다 값이 우선한다.
 */
public record SupplierUpdateCommand(
        String name,
        String managerName,
        String contactNumber,
        String email,
        String address,
        boolean clearEmail,
        boolean clearAddress
) {

    public boolean hasNoChanges() {
        return name == null
                && managerName == null
                && contactNumber == null
                && email == null && !clearEmail
                && address == null && !clearAddress;
    }
}

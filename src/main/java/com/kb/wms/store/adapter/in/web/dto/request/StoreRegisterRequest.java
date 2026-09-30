package com.kb.wms.store.adapter.in.web.dto.request;

import com.kb.wms.store.application.port.in.command.StoreRegisterCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/stores 요청 바디.
 */
public record StoreRegisterRequest(
        @NotBlank(message = "지점 코드는 필수 값입니다.")
        @Size(max = 30, message = "지점 코드는 최대 30자입니다.")
        String storeCode,

        @NotBlank(message = "지점명은 필수 값입니다.")
        @Size(max = 100, message = "지점명은 최대 100자입니다.")
        String storeName,

        @NotBlank(message = "지점 주소는 필수 값입니다.")
        @Size(max = 500, message = "지점 주소는 최대 500자입니다.")
        String address,

        @Size(max = 100, message = "지점 담당자명은 최대 100자입니다.")
        String contactName,

        @NotBlank(message = "지점 연락처는 필수 값입니다.")
        @Size(max = 30, message = "지점 연락처는 최대 30자입니다.")
        String contactNumber
) {

    public StoreRegisterCommand toCommand() {
        return new StoreRegisterCommand(storeCode, storeName, address, contactName, contactNumber);
    }
}

package com.kb.wms.inbound.adapter.in.web.dto.request;

import com.kb.wms.inbound.application.port.in.command.SupplierRegisterCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/suppliers 요청 바디.
 */
public record SupplierRegisterRequest(
        @NotBlank(message = "공급처 코드는 필수 값입니다.")
        @Size(max = 30, message = "공급처 코드는 최대 30자입니다.")
        String supplierCode,

        @NotBlank(message = "공급처명은 필수 값입니다.")
        @Size(max = 200, message = "공급처명은 최대 200자입니다.")
        String supplierName,

        @NotBlank(message = "공급처 담당자명은 필수 값입니다.")
        @Size(max = 100, message = "공급처 담당자명은 최대 100자입니다.")
        String managerName,

        @NotBlank(message = "공급처 연락처는 필수 값입니다.")
        @Size(max = 30, message = "공급처 연락처는 최대 30자입니다.")
        String contactNumber,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 최대 255자입니다.")
        String email,

        @Size(max = 500, message = "공급처 주소는 최대 500자입니다.")
        String address
) {

    public SupplierRegisterCommand toCommand() {
        return new SupplierRegisterCommand(supplierCode, supplierName, managerName, contactNumber, email, address);
    }
}

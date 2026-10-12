package com.kb.wms.inbound.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.kb.wms.common.validation.GeoCoordinates;
import com.kb.wms.common.validation.PhoneNumber;
import com.kb.wms.inbound.application.port.in.command.SupplierRegisterCommand;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
        @PhoneNumber
        String contactNumber,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 최대 255자입니다.")
        String email,

        @Size(max = 500, message = "공급처 주소는 최대 500자입니다.")
        String address,

        @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
        BigDecimal latitude,

        @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
        BigDecimal longitude
) {

    @AssertTrue(message = "위도와 경도는 함께 입력해야 합니다.")
    public boolean isCoordinatePairValid() {
        return GeoCoordinates.isPair(latitude, longitude);
    }

    public SupplierRegisterCommand toCommand() {
        return new SupplierRegisterCommand(supplierCode, supplierName, managerName, contactNumber, email, address,
                latitude, longitude);
    }
}

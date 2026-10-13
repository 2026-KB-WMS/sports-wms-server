package com.kb.wms.warehouse.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.kb.wms.common.validation.GeoCoordinates;
import com.kb.wms.common.validation.PhoneNumber;
import com.kb.wms.warehouse.application.port.in.command.WarehouseRegisterCommand;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/warehouses 요청 바디. 창고 코드는 받지 않고 주소의 시·도로 서버가 발급한다.
 */
public record WarehouseRegisterRequest(
        @NotBlank(message = "창고명은 필수 값입니다.")
        @Size(max = 100, message = "창고명은 최대 100자입니다.")
        String warehouseName,

        @NotBlank(message = "창고 주소는 필수 값입니다.")
        @Size(max = 500, message = "창고 주소는 최대 500자입니다.")
        String address,

        @Size(max = 30, message = "연락처는 최대 30자입니다.")
        @PhoneNumber
        String contactNumber,

        @NotNull(message = "전체 수용량은 필수 값입니다.")
        @DecimalMin(value = "0", inclusive = true, message = "전체 수용량은 0 이상이어야 합니다.")
        @Digits(integer = 11, fraction = 3, message = "전체 수용량은 소수 3자리까지만 입력할 수 있습니다.")
        BigDecimal totalCapacity,

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

    public WarehouseRegisterCommand toCommand() {
        return new WarehouseRegisterCommand(warehouseName, address, contactNumber, totalCapacity,
                latitude, longitude);
    }
}

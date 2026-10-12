package com.kb.wms.store.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.kb.wms.common.validation.GeoCoordinates;
import com.kb.wms.common.validation.PhoneNumber;
import com.kb.wms.store.application.port.in.command.StoreRegisterCommand;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
        @PhoneNumber
        String contactNumber,

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

    public StoreRegisterCommand toCommand() {
        return new StoreRegisterCommand(storeCode, storeName, address, contactName, contactNumber,
                latitude, longitude);
    }
}

package com.kb.wms.store.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.validation.GeoCoordinates;
import com.kb.wms.common.validation.PhoneNumber;
import com.kb.wms.store.application.port.in.command.StoreUpdateCommand;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/stores/{storeId} 요청 바디. 모든 필드는 선택이며, 값이 있는 필드만 수정한다.
 * storeCode와 운영 상태(isActive)는 이 API로 수정할 수 없다. 검증 애노테이션 대신
 * toCommand()에서 명시적으로 거부해, 명세대로 400 VALIDATION_ERROR로 응답한다.
 */
public record StoreUpdateRequest(
        @Size(max = 100, message = "지점명은 최대 100자입니다.")
        String storeName,

        @Size(max = 500, message = "지점 주소는 최대 500자입니다.")
        String address,

        @Size(max = 100, message = "지점 담당자명은 최대 100자입니다.")
        String contactName,

        @Size(max = 30, message = "지점 연락처는 최대 30자입니다.")
        @PhoneNumber
        String contactNumber,

        String storeCode,

        Boolean isActive,

        @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
        BigDecimal latitude,

        @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
        BigDecimal longitude
) {

    public StoreUpdateRequest(String storeName, String address, String contactName, String contactNumber,
                              String storeCode, Boolean isActive) {
        this(storeName, address, contactName, contactNumber, storeCode, isActive, null, null);
    }

    @AssertTrue(message = "위도와 경도는 함께 입력해야 합니다.")
    public boolean isCoordinatePairValid() {
        return GeoCoordinates.isPair(latitude, longitude);
    }

    public StoreUpdateCommand toCommand() {
        if (storeCode != null || isActive != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "storeCode, isActive는 이 API로 수정할 수 없습니다.");
        }
        return new StoreUpdateCommand(storeName, address, contactName, contactNumber,
                latitude, longitude);
    }
}

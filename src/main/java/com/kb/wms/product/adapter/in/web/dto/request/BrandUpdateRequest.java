package com.kb.wms.product.adapter.in.web.dto.request;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.command.BrandUpdateCommand;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/products/brands/{brandId} 요청 바디. 모든 필드는 선택이며, 값이 있는 필드만 수정한다.
 * brandCode는 코드가 없는 기존 브랜드에 한 번만 지정할 수 있다.
 */
public record BrandUpdateRequest(
        @Size(max = 100, message = "브랜드 이름은 최대 100자입니다.")
        String brandName,

        @Size(max = 500, message = "브랜드 설명은 최대 500자입니다.")
        String description,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "브랜드 코드는 영문 3자여야 합니다.")
        String brandCode
) {

    public BrandUpdateCommand toCommand(Long brandId) {
        if (brandName != null && brandName.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "브랜드 이름은 비워 둘 수 없습니다.");
        }
        return new BrandUpdateCommand(brandId, brandName, description, brandCode);
    }
}

package com.kb.wms.product.adapter.in.web.dto.request;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.command.BrandUpdateCommand;

import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/products/brands/{brandId} 요청 바디. 모든 필드는 선택이며, 값이 있는 필드만 수정한다.
 */
public record BrandUpdateRequest(
        @Size(max = 100, message = "브랜드 이름은 최대 100자입니다.")
        String brandName,

        @Size(max = 500, message = "브랜드 설명은 최대 500자입니다.")
        String description
) {

    public BrandUpdateCommand toCommand(Long brandId) {
        if (brandName != null && brandName.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "브랜드 이름은 비워 둘 수 없습니다.");
        }
        return new BrandUpdateCommand(brandId, brandName, description);
    }
}

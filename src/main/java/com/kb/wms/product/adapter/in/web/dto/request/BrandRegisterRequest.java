package com.kb.wms.product.adapter.in.web.dto.request;

import com.kb.wms.product.application.port.in.command.BrandRegisterCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/products/brands 요청 바디.
 */
public record BrandRegisterRequest(
        @NotBlank(message = "브랜드 이름은 필수 값입니다.")
        @Size(max = 100, message = "브랜드 이름은 최대 100자입니다.")
        String brandName,

        @Size(max = 500, message = "브랜드 설명은 최대 500자입니다.")
        String description
) {

    public BrandRegisterCommand toCommand() {
        return new BrandRegisterCommand(brandName, description);
    }
}

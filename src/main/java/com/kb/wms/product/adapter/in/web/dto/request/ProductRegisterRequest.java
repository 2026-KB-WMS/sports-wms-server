package com.kb.wms.product.adapter.in.web.dto.request;

import com.kb.wms.product.application.port.in.command.ProductRegisterCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/products 요청 바디.
 */
public record ProductRegisterRequest(
        @NotNull(message = "브랜드 ID는 필수 값입니다.")
        Long brandId,

        @NotNull(message = "카테고리 ID는 필수 값입니다.")
        Long categoryId,

        @NotBlank(message = "상품 코드는 필수 값입니다.")
        @Size(max = 50, message = "상품 코드는 최대 50자입니다.")
        String productCode,

        @NotBlank(message = "상품명은 필수 값입니다.")
        @Size(max = 200, message = "상품명은 최대 200자입니다.")
        String productName,

        @Size(max = 1000, message = "상품 설명은 최대 1000자입니다.")
        String description
) {

    public ProductRegisterCommand toCommand() {
        return new ProductRegisterCommand(brandId, categoryId, productCode, productName, description);
    }
}

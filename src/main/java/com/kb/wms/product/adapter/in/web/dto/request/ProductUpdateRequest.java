package com.kb.wms.product.adapter.in.web.dto.request;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.command.ProductUpdateCommand;

import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/products/{productId} 요청 바디. 모든 필드는 선택이며, 값이 있는 필드만 수정한다.
 * productCode는 불변이므로 이 API로 수정할 수 없다. 검증 애노테이션 대신
 * toCommand()에서 명시적으로 거부해, 명세대로 400 VALIDATION_ERROR로 응답한다.
 */
public record ProductUpdateRequest(
        @Size(max = 200, message = "상품명은 최대 200자입니다.")
        String productName,

        @Size(max = 1000, message = "상품 설명은 최대 1000자입니다.")
        String description,

        Long brandId,

        Long categoryId,

        Boolean isActive,

        String productCode
) {

    public ProductUpdateCommand toCommand(Long productId) {
        if (productCode != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "productCode는 이 API로 수정할 수 없습니다.");
        }
        return new ProductUpdateCommand(productId, productName, description, brandId, categoryId, isActive);
    }
}

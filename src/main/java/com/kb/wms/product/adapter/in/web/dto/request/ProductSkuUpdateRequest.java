package com.kb.wms.product.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.command.ProductSkuUpdateCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/products/skus/{skuId} 요청 바디. 모든 필드는 선택이며, 값이 있는 필드만 수정한다.
 * skuCode·productId·unit·isActive는 이 API로 수정할 수 없다(상태는 /status). 검증 애노테이션 대신
 * toCommand()에서 명시적으로 거부해, 명세대로 400 VALIDATION_ERROR로 응답한다.
 */
public record ProductSkuUpdateRequest(
        @Size(min = 1, max = 200, message = "SKU 명칭은 1자 이상 200자 이하입니다.")
        String skuName,

        @Size(max = 100, message = "바코드는 최대 100자입니다.")
        String barcode,

        @DecimalMin(value = "0", inclusive = true, message = "중량은 0 이상이어야 합니다.")
        BigDecimal weight,

        @PositiveOrZero(message = "매입 단가는 0 이상이어야 합니다.")
        BigDecimal currentPurchasePrice,

        @PositiveOrZero(message = "공급 단가는 0 이상이어야 합니다.")
        BigDecimal currentSupplyPrice,

        @PositiveOrZero(message = "안전 재고 수량은 0 이상이어야 합니다.")
        Long safetyStockQuantity,

        String skuCode,

        Long productId,

        String unit,

        Boolean isActive
) {

    public ProductSkuUpdateCommand toCommand(Long skuId) {
        if (skuCode != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "skuCode는 이 API로 수정할 수 없습니다.");
        }
        if (productId != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "productId는 이 API로 수정할 수 없습니다.");
        }
        if (unit != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "unit은 이 API로 수정할 수 없습니다.");
        }
        if (isActive != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "상태는 PATCH /products/skus/{skuId}/status로 변경합니다.");
        }
        return new ProductSkuUpdateCommand(skuId, skuName, barcode, weight, currentPurchasePrice,
                currentSupplyPrice, safetyStockQuantity);
    }
}

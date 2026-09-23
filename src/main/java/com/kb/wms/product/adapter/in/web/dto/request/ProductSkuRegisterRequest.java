package com.kb.wms.product.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/products/skus 요청 바디.
 */
public record ProductSkuRegisterRequest(
        @NotNull(message = "상품 ID는 필수 값입니다.")
        Long productId,

        @NotBlank(message = "SKU 코드는 필수 값입니다.")
        @Size(max = 50, message = "SKU 코드는 최대 50자입니다.")
        String skuCode,

        @Size(max = 100, message = "바코드는 최대 100자입니다.")
        String barcode,

        @NotBlank(message = "SKU 명칭은 필수 값입니다.")
        @Size(max = 200, message = "SKU 명칭은 최대 200자입니다.")
        String skuName,

        @DecimalMin(value = "0", inclusive = true, message = "중량은 0 이상이어야 합니다.")
        BigDecimal weight,

        @PositiveOrZero(message = "매입 단가는 0 이상이어야 합니다.")
        BigDecimal currentPurchasePrice,

        @PositiveOrZero(message = "공급 단가는 0 이상이어야 합니다.")
        BigDecimal currentSupplyPrice,

        @Size(max = 20, message = "수량 단위는 최대 20자입니다.")
        String unit,

        @PositiveOrZero(message = "안전 재고 수량은 0 이상이어야 합니다.")
        Long safetyStockQuantity
) {

    public ProductSkuRegisterCommand toCommand() {
        return new ProductSkuRegisterCommand(
                productId, skuCode, barcode, skuName, weight,
                currentPurchasePrice, currentSupplyPrice, unit, safetyStockQuantity);
    }
}

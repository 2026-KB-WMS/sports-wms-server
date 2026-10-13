package com.kb.wms.product.adapter.in.web.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/products/skus 요청 바디. SKU 코드·명칭은 서버가 상품·옵션 값으로 만들어 항상 응답으로만 준다(ADR-026).
 */
public record ProductSkuRegisterRequest(
        @NotNull(message = "상품 ID는 필수 값입니다.")
        Long productId,

        @Size(max = 100, message = "바코드는 최대 100자입니다.")
        String barcode,

        @DecimalMin(value = "0", inclusive = true, message = "중량은 0 이상이어야 합니다.")
        BigDecimal weight,

        @NotNull(message = "매입 단가는 필수 값입니다.")
        @PositiveOrZero(message = "매입 단가는 0 이상이어야 합니다.")
        BigDecimal currentPurchasePrice,

        @NotNull(message = "공급 단가는 필수 값입니다.")
        @PositiveOrZero(message = "공급 단가는 0 이상이어야 합니다.")
        BigDecimal currentSupplyPrice,

        @Size(max = 20, message = "수량 단위는 최대 20자입니다.")
        String unit,

        @PositiveOrZero(message = "안전 재고 수량은 0 이상이어야 합니다.")
        Long safetyStockQuantity,

        List<Long> optionValueIds
) {

    public ProductSkuRegisterCommand toCommand() {
        return new ProductSkuRegisterCommand(
                productId, barcode, weight,
                currentPurchasePrice, currentSupplyPrice, unit, safetyStockQuantity, optionValueIds);
    }
}

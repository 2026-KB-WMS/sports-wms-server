package com.kb.wms.inbound.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * POST /api/v1/products/skus/{skuId}/suppliers 요청 바디.
 */
public record SkuSupplierRegisterRequest(
        @NotNull(message = "공급처 ID는 필수 값입니다.")
        Long supplierId
) {
}

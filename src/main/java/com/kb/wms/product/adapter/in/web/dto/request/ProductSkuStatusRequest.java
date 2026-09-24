package com.kb.wms.product.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * PATCH /api/v1/products/skus/{skuId}/status 요청 바디.
 */
public record ProductSkuStatusRequest(
        @NotNull(message = "isActive는 필수입니다.")
        Boolean isActive
) {
}

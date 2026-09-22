package com.kb.wms.product.adapter.in.web.dto.request;

import java.util.List;

import com.kb.wms.product.application.port.in.command.SkuOptionConnectCommand;

import jakarta.validation.constraints.NotEmpty;

/**
 * POST /api/v1/products/skus/{skuId}/options 요청 바디.
 */
public record SkuOptionConnectRequest(
        @NotEmpty(message = "연결할 옵션 값 ID는 1개 이상이어야 합니다.")
        List<Long> optionValueIds
) {

    public SkuOptionConnectCommand toCommand(Long skuId) {
        return new SkuOptionConnectCommand(skuId, optionValueIds);
    }
}

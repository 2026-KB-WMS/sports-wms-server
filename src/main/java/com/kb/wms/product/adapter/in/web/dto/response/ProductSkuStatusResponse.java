package com.kb.wms.product.adapter.in.web.dto.response;

import com.kb.wms.product.domain.entity.ProductSku;

public record ProductSkuStatusResponse(
        Long skuId,
        boolean isActive
) {

    public static ProductSkuStatusResponse from(ProductSku sku) {
        return new ProductSkuStatusResponse(sku.getSkuId(), sku.isActive());
    }
}

package com.kb.wms.inbound.adapter.in.web.dto.response;

/**
 * DELETE /api/v1/products/skus/{skuId}/suppliers/{supplierId} (매핑 삭제) 응답.
 */
public record SkuSupplierRemoveResponse(
        Long skuId,
        Long supplierId
) {
}

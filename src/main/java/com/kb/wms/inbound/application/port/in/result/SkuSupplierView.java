package com.kb.wms.inbound.application.port.in.result;

import java.time.LocalDateTime;

/**
 * SKU에 매핑된 공급처 한 곳 (GET /api/v1/products/skus/{skuId}/suppliers).
 *
 * @param createdAt 매핑을 등록한 일시
 */
public record SkuSupplierView(
        Long supplierId,
        String supplierCode,
        String supplierName,
        boolean isActive,
        LocalDateTime createdAt
) {
}

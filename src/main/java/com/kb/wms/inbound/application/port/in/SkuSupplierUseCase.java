package com.kb.wms.inbound.application.port.in;

import java.util.List;

import com.kb.wms.inbound.application.port.in.result.SkuSupplierView;

/**
 * SKU↔공급처 매핑 조회/등록/삭제 유스케이스.
 * GET, POST /api/v1/products/skus/{skuId}/suppliers, DELETE .../{supplierId}
 */
public interface SkuSupplierUseCase {

    /** 매핑 등록 순으로 돌려준다. SKU가 없으면 404 SKU_NOT_FOUND. */
    List<SkuSupplierView> getSuppliers(Long skuId);

    /**
     * 공급처를 SKU에 매핑한다. SKU·공급처가 없으면 404, 비활성 공급처는 409 SUPPLIER_INACTIVE,
     * 이미 매핑돼 있으면 409 SKU_SUPPLIER_ALREADY_MAPPED.
     */
    SkuSupplierView registerSupplier(Long skuId, Long supplierId);

    /** 매핑을 삭제한다. 이미 확정된 발주에는 영향이 없다. 매핑이 없으면 404 SKU_SUPPLIER_NOT_FOUND. */
    void removeSupplier(Long skuId, Long supplierId);
}

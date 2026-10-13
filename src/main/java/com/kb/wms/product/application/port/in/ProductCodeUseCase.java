package com.kb.wms.product.application.port.in;

import java.util.List;

import com.kb.wms.product.application.port.in.result.CodeItem;

/**
 * 상품 도메인 고정 코드 목록 조회 유스케이스.
 * GET /api/v1/products/skus/units
 */
public interface ProductCodeUseCase {

    /** SKU 수량 단위 허용 목록. */
    List<CodeItem> getSkuUnits();
}

package com.kb.wms.product.application.port.in;

import com.kb.wms.product.application.port.in.query.ProductSkuSearchCondition;
import java.util.List;

import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;
import com.kb.wms.product.application.port.in.command.SkuOptionConnectCommand;
import com.kb.wms.product.application.port.in.result.SkuOptionSummary;
import com.kb.wms.product.domain.entity.ProductSku;

/**
 * SKU 등록/조회/옵션 연결 유스케이스.
 * POST /api/v1/products/skus, GET /api/v1/products/skus, GET /api/v1/products/skus/{skuId},
 * POST /api/v1/products/skus/{skuId}/options
 */
public interface ProductSkuUseCase {

    ProductSku registerSku(ProductSkuRegisterCommand command);

    /** 필터의 상품·브랜드·카테고리가 없으면 PRODUCT_NOT_FOUND·BRAND_NOT_FOUND·CATEGORY_NOT_FOUND */
    List<ProductSku> getSkus(ProductSkuSearchCondition condition);

    ProductSku getSku(Long skuId);

    void connectOptions(SkuOptionConnectCommand command);

    List<SkuOptionSummary> getSkuOptions(Long skuId);
}

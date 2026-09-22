package com.kb.wms.product.application.port.in;

import java.util.List;

import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;
import com.kb.wms.product.application.port.in.command.SkuOptionConnectCommand;
import com.kb.wms.product.domain.entity.ProductSku;

/**
 * SKU 등록/조회/옵션 연결 유스케이스.
 * POST /api/v1/products/skus, GET /api/v1/products/skus, GET /api/v1/products/skus/{skuId},
 * POST /api/v1/products/skus/{skuId}/options
 */
public interface ProductSkuUseCase {

    ProductSku registerSku(ProductSkuRegisterCommand command);

    List<ProductSku> getSkus(Long productId);

    ProductSku getSku(Long skuId);

    void connectOptions(SkuOptionConnectCommand command);
}

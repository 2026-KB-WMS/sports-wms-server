package com.kb.wms.product.application.port.in;

import com.kb.wms.product.application.port.in.query.ProductSearchCondition;
import java.util.List;

import com.kb.wms.product.application.port.in.command.ProductRegisterCommand;
import com.kb.wms.product.application.port.in.command.ProductUpdateCommand;
import com.kb.wms.product.domain.entity.Product;

/**
 * 상품 등록/조회 유스케이스.
 * POST /api/v1/products, GET /api/v1/products, GET /api/v1/products/{productId},
 * PATCH /api/v1/products/{productId}
 */
public interface ProductUseCase {

    Product registerProduct(ProductRegisterCommand command);

    Product updateProduct(ProductUpdateCommand command);

    /** 필터의 브랜드·카테고리가 없으면 BRAND_NOT_FOUND·CATEGORY_NOT_FOUND */
    List<Product> getProducts(ProductSearchCondition condition);

    Product getProduct(Long productId);
}

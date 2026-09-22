package com.kb.wms.product.application.port.in;

import java.util.List;

import com.kb.wms.product.application.port.in.command.ProductRegisterCommand;
import com.kb.wms.product.domain.entity.Product;

/**
 * 상품 등록/조회 유스케이스.
 * POST /api/v1/products, GET /api/v1/products, GET /api/v1/products/{productId}
 */
public interface ProductUseCase {

    Product registerProduct(ProductRegisterCommand command);

    List<Product> getProducts(Long brandId, Long categoryId);

    Product getProduct(Long productId);
}

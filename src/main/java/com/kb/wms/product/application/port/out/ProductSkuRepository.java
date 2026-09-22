package com.kb.wms.product.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.product.domain.entity.ProductSku;

/**
 * 상품 SKU 영속성 아웃바운드 포트.
 */
public interface ProductSkuRepository {

    ProductSku save(ProductSku productSku);

    Optional<ProductSku> findById(Long skuId);

    /**
     * productId가 null이면 조건을 무시하고 조회한다.
     */
    List<ProductSku> findAll(Long productId);

    boolean existsBySkuCode(String skuCode);

    boolean existsByBarcode(String barcode);
}

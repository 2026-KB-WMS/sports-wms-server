package com.kb.wms.product.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.product.application.port.in.query.ProductSkuSearchCondition;
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

    /**
     * 조건이 null이면 해당 조건은 무시한다. 브랜드·카테고리는 SKU가 속한 상품 기준이며, 생성 일시 내림차순.
     */
    List<ProductSku> search(ProductSkuSearchCondition condition);

    boolean existsBySkuCode(String skuCode);

    boolean existsByBarcode(String barcode);
}

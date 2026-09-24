package com.kb.wms.product.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.product.application.port.in.query.ProductSearchCondition;
import com.kb.wms.product.domain.entity.Product;

/**
 * 상품 영속성 아웃바운드 포트.
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long productId);

    /**
     * 조건이 null이면 해당 조건은 무시한다. 생성 일시 내림차순.
     */
    List<Product> search(ProductSearchCondition condition);

    boolean existsByProductCode(String productCode);
}

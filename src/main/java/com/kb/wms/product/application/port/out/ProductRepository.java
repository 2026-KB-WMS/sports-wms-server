package com.kb.wms.product.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.product.domain.entity.Product;

/**
 * 상품 영속성 아웃바운드 포트.
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long productId);

    /**
     * brandId·categoryId가 null이면 해당 조건은 무시하고 조회한다.
     */
    List<Product> findAll(Long brandId, Long categoryId);

    boolean existsByProductCode(String productCode);
}

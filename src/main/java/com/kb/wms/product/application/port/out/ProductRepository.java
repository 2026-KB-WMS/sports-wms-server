package com.kb.wms.product.application.port.out;

import java.util.Optional;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.product.application.port.in.query.ProductSearchCondition;
import com.kb.wms.product.domain.entity.Product;

/**
 * 상품 영속성 아웃바운드 포트.
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long productId);

    /** 없는 ID는 결과에서 빠진다. SKU 목록에서 상품명을 한 번에 읽을 때 쓴다. */
    java.util.List<Product> findByIdIn(java.util.Collection<Long> productIds);

    /**
     * 조건이 null이면 해당 조건은 무시한다. 생성 일시 내림차순.
     */
    PageResult<Product> search(ProductSearchCondition condition, PageCondition page);

    boolean existsByProductCode(String productCode);
}

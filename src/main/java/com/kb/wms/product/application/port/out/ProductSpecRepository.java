package com.kb.wms.product.application.port.out;

import java.util.List;

import com.kb.wms.product.domain.entity.ProductSpec;

/**
 * 상품 스펙 값 영속성 아웃바운드 포트.
 */
public interface ProductSpecRepository {

    List<ProductSpec> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    /** 상품의 스펙을 모두 지우고 주어진 옵션 값들로 바꾼다. */
    void replaceAll(Long productId, List<Long> optionValueIds);
}

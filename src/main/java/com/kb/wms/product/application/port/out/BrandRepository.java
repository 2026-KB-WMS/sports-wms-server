package com.kb.wms.product.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.product.application.port.in.query.BrandSearchCondition;
import com.kb.wms.product.domain.entity.Brand;

/**
 * 브랜드 영속성 아웃바운드 포트.
 */
public interface BrandRepository {

    Brand save(Brand brand);

    Optional<Brand> findById(Long brandId);

    /** 브랜드명 오름차순 */
    List<Brand> search(BrandSearchCondition condition);

    boolean existsByName(String name);
}

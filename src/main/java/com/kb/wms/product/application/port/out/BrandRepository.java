package com.kb.wms.product.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.product.domain.entity.Brand;

/**
 * 브랜드 영속성 아웃바운드 포트.
 */
public interface BrandRepository {

    Brand save(Brand brand);

    Optional<Brand> findById(Long brandId);

    List<Brand> findAll();

    boolean existsByName(String name);
}

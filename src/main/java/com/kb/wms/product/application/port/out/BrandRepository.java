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

    /** 행을 비관적 락으로 잠가 읽는다. 상품 코드 채번을 브랜드 단위로 직렬화할 때 쓴다. */
    Optional<Brand> findByIdForUpdate(Long brandId);

    /** 브랜드명 오름차순 */
    List<Brand> search(BrandSearchCondition condition);

    boolean existsByName(String name);

    boolean existsByBrandCode(String brandCode);

    /** 다른 브랜드(excludeBrandId 제외)가 같은 코드를 쓰는지 */
    boolean existsByBrandCodeAndBrandIdNot(String brandCode, Long excludeBrandId);

    /** 다른 브랜드(excludeBrandId 제외)가 같은 이름을 쓰는지 */
    boolean existsByNameAndBrandIdNot(String name, Long excludeBrandId);
}

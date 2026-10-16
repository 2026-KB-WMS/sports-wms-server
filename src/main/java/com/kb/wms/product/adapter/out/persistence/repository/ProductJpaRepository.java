package com.kb.wms.product.adapter.out.persistence.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.product.adapter.out.persistence.entity.ProductJpaEntity;
import com.kb.wms.product.domain.enums.ProductStatus;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {

    boolean existsByProductCode(String productCode);

    /** 정렬·페이징은 {@code Pageable}이 정한다. */
    @Query(value = """
            select p from ProductJpaEntity p
            where (:brandId is null or p.brandId = :brandId)
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:status is null or p.status = :status)
              and (:keyword is null
                   or lower(p.name) like lower(concat('%', :keyword, '%'))
                   or lower(p.productCode) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
            select count(p) from ProductJpaEntity p
            where (:brandId is null or p.brandId = :brandId)
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:status is null or p.status = :status)
              and (:keyword is null
                   or lower(p.name) like lower(concat('%', :keyword, '%'))
                   or lower(p.productCode) like lower(concat('%', :keyword, '%')))
            """)
    Page<ProductJpaEntity> search(@Param("brandId") Long brandId,
                                  @Param("categoryId") Long categoryId,
                                  @Param("keyword") String keyword,
                                  @Param("status") ProductStatus status,
                                  Pageable pageable);
}

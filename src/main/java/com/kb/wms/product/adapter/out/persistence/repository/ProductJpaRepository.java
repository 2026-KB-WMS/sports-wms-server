package com.kb.wms.product.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.product.adapter.out.persistence.entity.ProductJpaEntity;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {

    boolean existsByProductCode(String productCode);

    @Query("""
            select p from ProductJpaEntity p
            where (:brandId is null or p.brandId = :brandId)
              and (:categoryId is null or p.categoryId = :categoryId)
            """)
    List<ProductJpaEntity> findAllByFilter(@Param("brandId") Long brandId, @Param("categoryId") Long categoryId);
}

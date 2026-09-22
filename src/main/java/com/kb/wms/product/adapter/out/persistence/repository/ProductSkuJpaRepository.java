package com.kb.wms.product.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;

public interface ProductSkuJpaRepository extends JpaRepository<ProductSkuJpaEntity, Long> {

    boolean existsBySkuCode(String skuCode);

    boolean existsByBarcode(String barcode);

    @Query("""
            select s from ProductSkuJpaEntity s
            where (:productId is null or s.productId = :productId)
            """)
    List<ProductSkuJpaEntity> findAllByFilter(@Param("productId") Long productId);
}

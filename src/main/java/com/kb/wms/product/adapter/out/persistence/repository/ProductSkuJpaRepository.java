package com.kb.wms.product.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.domain.enums.ProductStatus;

public interface ProductSkuJpaRepository extends JpaRepository<ProductSkuJpaEntity, Long> {

    boolean existsBySkuCode(String skuCode);

    boolean existsByBarcode(String barcode);

    @Query("""
            select s from ProductSkuJpaEntity s
            where (:productId is null or s.productId = :productId)
            """)
    List<ProductSkuJpaEntity> findAllByFilter(@Param("productId") Long productId);

    @Query("""
            select s from ProductSkuJpaEntity s
            join ProductJpaEntity p on p.productId = s.productId
            where (:productId is null or s.productId = :productId)
              and (:brandId is null or p.brandId = :brandId)
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:status is null or s.status = :status)
              and (:keyword is null
                   or lower(s.skuCode) like lower(concat('%', :keyword, '%'))
                   or lower(s.barcode) like lower(concat('%', :keyword, '%'))
                   or lower(s.name) like lower(concat('%', :keyword, '%')))
            order by s.createdAt desc, s.skuId desc
            """)
    List<ProductSkuJpaEntity> search(@Param("productId") Long productId,
                                     @Param("brandId") Long brandId,
                                     @Param("categoryId") Long categoryId,
                                     @Param("keyword") String keyword,
                                     @Param("status") ProductStatus status);
}

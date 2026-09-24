package com.kb.wms.product.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.product.adapter.out.persistence.entity.BrandJpaEntity;
import com.kb.wms.product.domain.enums.ProductStatus;

public interface BrandJpaRepository extends JpaRepository<BrandJpaEntity, Long> {

    boolean existsByName(String name);

    @Query("""
            select b from BrandJpaEntity b
            where (:status is null or b.status = :status)
              and (:keyword is null or lower(b.name) like lower(concat('%', :keyword, '%')))
            order by b.name, b.brandId
            """)
    List<BrandJpaEntity> search(@Param("keyword") String keyword, @Param("status") ProductStatus status);
}

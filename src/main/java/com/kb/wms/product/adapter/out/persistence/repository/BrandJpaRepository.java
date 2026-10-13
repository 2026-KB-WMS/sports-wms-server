package com.kb.wms.product.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.product.adapter.out.persistence.entity.BrandJpaEntity;
import com.kb.wms.product.domain.enums.ProductStatus;

import jakarta.persistence.LockModeType;

public interface BrandJpaRepository extends JpaRepository<BrandJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BrandJpaEntity b where b.brandId = :brandId")
    Optional<BrandJpaEntity> findByIdForUpdate(@Param("brandId") Long brandId);

    boolean existsByName(String name);

    boolean existsByBrandCode(String brandCode);

    boolean existsByBrandCodeAndBrandIdNot(String brandCode, Long brandId);

    boolean existsByNameAndBrandIdNot(String name, Long brandId);

    @Query("""
            select b from BrandJpaEntity b
            where (:status is null or b.status = :status)
              and (:keyword is null
                   or lower(b.name) like lower(concat('%', :keyword, '%'))
                   or lower(b.brandCode) like lower(concat('%', :keyword, '%')))
            order by b.name, b.brandId
            """)
    List<BrandJpaEntity> search(@Param("keyword") String keyword, @Param("status") ProductStatus status);
}

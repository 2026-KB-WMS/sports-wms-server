package com.kb.wms.product.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.product.adapter.out.persistence.entity.ProductCodeSequenceJpaEntity;

import jakarta.persistence.LockModeType;

public interface ProductCodeSequenceJpaRepository
        extends JpaRepository<ProductCodeSequenceJpaEntity, ProductCodeSequenceJpaEntity.Key> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ProductCodeSequenceJpaEntity s where s.brandId = :brandId and s.categoryId = :categoryId")
    Optional<ProductCodeSequenceJpaEntity> findForUpdate(@Param("brandId") Long brandId,
                                                          @Param("categoryId") Long categoryId);
}

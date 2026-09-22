package com.kb.wms.product.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kb.wms.product.adapter.out.persistence.entity.BrandJpaEntity;

public interface BrandJpaRepository extends JpaRepository<BrandJpaEntity, Long> {

    boolean existsByName(String name);
}

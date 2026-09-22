package com.kb.wms.product.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kb.wms.product.adapter.out.persistence.entity.CategoryJpaEntity;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {

    boolean existsByCategoryCode(String categoryCode);
}

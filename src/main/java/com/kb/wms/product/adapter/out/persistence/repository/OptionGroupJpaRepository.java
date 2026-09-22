package com.kb.wms.product.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kb.wms.product.adapter.out.persistence.entity.OptionGroupJpaEntity;

public interface OptionGroupJpaRepository extends JpaRepository<OptionGroupJpaEntity, Long> {

    boolean existsByName(String name);
}

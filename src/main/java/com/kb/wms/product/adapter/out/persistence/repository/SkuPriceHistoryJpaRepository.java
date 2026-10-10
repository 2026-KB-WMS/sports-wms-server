package com.kb.wms.product.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kb.wms.product.adapter.out.persistence.entity.SkuPriceHistoryJpaEntity;

public interface SkuPriceHistoryJpaRepository extends JpaRepository<SkuPriceHistoryJpaEntity, Long> {
}

package com.kb.wms.inbound.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.kb.wms.inbound.adapter.out.persistence.entity.SkuSupplierJpaEntity;

public interface SkuSupplierJpaRepository extends JpaRepository<SkuSupplierJpaEntity, Long> {

    List<SkuSupplierJpaEntity> findBySkuIdOrderBySkuSupplierIdAsc(Long skuId);

    List<SkuSupplierJpaEntity> findBySkuIdIn(Collection<Long> skuIds);

    boolean existsBySkuIdAndSupplierId(Long skuId, Long supplierId);

    @Modifying
    long deleteBySkuIdAndSupplierId(Long skuId, Long supplierId);
}

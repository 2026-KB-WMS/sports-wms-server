package com.kb.wms.product.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kb.wms.product.adapter.out.persistence.entity.SkuOptionValueId;
import com.kb.wms.product.adapter.out.persistence.entity.SkuOptionValueJpaEntity;

public interface SkuOptionValueJpaRepository extends JpaRepository<SkuOptionValueJpaEntity, SkuOptionValueId> {

    List<SkuOptionValueJpaEntity> findById_SkuId(Long skuId);

    boolean existsById_SkuIdAndId_OptionValueId(Long skuId, Long optionValueId);
}

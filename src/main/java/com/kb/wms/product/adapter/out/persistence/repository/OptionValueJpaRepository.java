package com.kb.wms.product.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kb.wms.product.adapter.out.persistence.entity.OptionValueJpaEntity;

public interface OptionValueJpaRepository extends JpaRepository<OptionValueJpaEntity, Long> {

    List<OptionValueJpaEntity> findByOptionGroupId(Long optionGroupId);

    boolean existsByOptionGroupIdAndValue(Long optionGroupId, String value);
}

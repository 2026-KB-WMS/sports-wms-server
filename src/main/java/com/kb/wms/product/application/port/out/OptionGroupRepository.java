package com.kb.wms.product.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.product.domain.entity.OptionGroup;

/**
 * 옵션 그룹 영속성 아웃바운드 포트.
 */
public interface OptionGroupRepository {

    OptionGroup save(OptionGroup optionGroup);

    Optional<OptionGroup> findById(Long optionGroupId);

    List<OptionGroup> findAll();

    boolean existsByName(String name);
}

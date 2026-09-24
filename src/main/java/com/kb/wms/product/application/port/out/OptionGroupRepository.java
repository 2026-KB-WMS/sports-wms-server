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

    /** 여러 옵션 그룹을 쿼리 한 번으로 조회한다. 없는 ID는 결과에서 빠진다. */
    List<OptionGroup> findAllByIds(java.util.Collection<Long> optionGroupIds);

    List<OptionGroup> findAll();

    boolean existsByName(String name);
}

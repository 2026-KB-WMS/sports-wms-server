package com.kb.wms.product.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.product.domain.entity.OptionValue;

/**
 * 옵션 값 영속성 아웃바운드 포트.
 */
public interface OptionValueRepository {

    OptionValue save(OptionValue optionValue);

    Optional<OptionValue> findById(Long optionValueId);

    List<OptionValue> findByOptionGroupId(Long optionGroupId);

    boolean existsByOptionGroupIdAndValue(Long optionGroupId, String value);
}

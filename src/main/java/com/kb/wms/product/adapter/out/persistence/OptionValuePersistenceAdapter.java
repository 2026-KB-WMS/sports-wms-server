package com.kb.wms.product.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.adapter.out.persistence.entity.OptionValueJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.OptionValueJpaRepository;
import com.kb.wms.product.domain.entity.OptionValue;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OptionValuePersistenceAdapter implements OptionValueRepository {

    private final OptionValueJpaRepository optionValueJpaRepository;

    @Override
    public OptionValue save(OptionValue optionValue) {
        OptionValueJpaEntity saved = optionValueJpaRepository.save(OptionValueJpaEntity.fromDomain(optionValue));
        return saved.toDomain();
    }

    @Override
    public Optional<OptionValue> findById(Long optionValueId) {
        return optionValueJpaRepository.findById(optionValueId).map(OptionValueJpaEntity::toDomain);
    }

    @Override
    public List<OptionValue> findByOptionGroupId(Long optionGroupId) {
        return optionValueJpaRepository.findByOptionGroupId(optionGroupId).stream()
                .map(OptionValueJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByOptionGroupIdAndValue(Long optionGroupId, String value) {
        return optionValueJpaRepository.existsByOptionGroupIdAndValue(optionGroupId, value);
    }
}

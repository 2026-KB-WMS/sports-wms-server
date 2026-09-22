package com.kb.wms.product.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.adapter.out.persistence.entity.OptionGroupJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.OptionGroupJpaRepository;
import com.kb.wms.product.domain.entity.OptionGroup;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OptionGroupPersistenceAdapter implements OptionGroupRepository {

    private final OptionGroupJpaRepository optionGroupJpaRepository;

    @Override
    public OptionGroup save(OptionGroup optionGroup) {
        OptionGroupJpaEntity saved = optionGroupJpaRepository.save(OptionGroupJpaEntity.fromDomain(optionGroup));
        return saved.toDomain();
    }

    @Override
    public Optional<OptionGroup> findById(Long optionGroupId) {
        return optionGroupJpaRepository.findById(optionGroupId).map(OptionGroupJpaEntity::toDomain);
    }

    @Override
    public List<OptionGroup> findAll() {
        return optionGroupJpaRepository.findAll().stream()
                .map(OptionGroupJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByName(String name) {
        return optionGroupJpaRepository.existsByName(name);
    }
}

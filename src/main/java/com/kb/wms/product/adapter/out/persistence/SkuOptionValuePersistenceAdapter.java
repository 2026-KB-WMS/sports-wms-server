package com.kb.wms.product.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kb.wms.product.application.port.out.SkuOptionValueRepository;
import com.kb.wms.product.adapter.out.persistence.entity.SkuOptionValueJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.SkuOptionValueJpaRepository;
import com.kb.wms.product.domain.entity.SkuOptionValue;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SkuOptionValuePersistenceAdapter implements SkuOptionValueRepository {

    private final SkuOptionValueJpaRepository skuOptionValueJpaRepository;

    @Override
    public SkuOptionValue save(SkuOptionValue skuOptionValue) {
        SkuOptionValueJpaEntity saved = skuOptionValueJpaRepository.save(SkuOptionValueJpaEntity.fromDomain(skuOptionValue));
        return saved.toDomain();
    }

    @Override
    public List<SkuOptionValue> findBySkuId(Long skuId) {
        return skuOptionValueJpaRepository.findById_SkuId(skuId).stream()
                .map(SkuOptionValueJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySkuIdAndOptionValueId(Long skuId, Long optionValueId) {
        return skuOptionValueJpaRepository.existsById_SkuIdAndId_OptionValueId(skuId, optionValueId);
    }
}

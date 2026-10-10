package com.kb.wms.product.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.kb.wms.product.adapter.out.persistence.entity.SkuPriceHistoryJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.SkuPriceHistoryJpaRepository;
import com.kb.wms.product.application.port.out.SkuPriceHistoryRepository;
import com.kb.wms.product.domain.entity.SkuPriceHistory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SkuPriceHistoryPersistenceAdapter implements SkuPriceHistoryRepository {

    private final SkuPriceHistoryJpaRepository skuPriceHistoryJpaRepository;

    @Override
    public SkuPriceHistory save(SkuPriceHistory history) {
        return skuPriceHistoryJpaRepository.save(SkuPriceHistoryJpaEntity.fromDomain(history)).toDomain();
    }
}

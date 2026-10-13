package com.kb.wms.product.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.kb.wms.product.adapter.out.persistence.entity.ProductCodeSequenceJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductCodeSequenceJpaRepository;
import com.kb.wms.product.application.port.out.ProductCodeSequenceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductCodeSequencePersistenceAdapter implements ProductCodeSequenceRepository {

    private final ProductCodeSequenceJpaRepository sequenceJpaRepository;

    @Override
    public long next(Long brandId, Long categoryId) {
        ProductCodeSequenceJpaEntity sequence = sequenceJpaRepository.findForUpdate(brandId, categoryId)
                .orElseGet(() -> sequenceJpaRepository.saveAndFlush(
                        new ProductCodeSequenceJpaEntity(brandId, categoryId)));
        return sequence.next();
    }
}

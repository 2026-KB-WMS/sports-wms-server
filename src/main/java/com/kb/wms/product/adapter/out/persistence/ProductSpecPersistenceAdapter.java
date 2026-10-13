package com.kb.wms.product.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kb.wms.product.adapter.out.persistence.entity.ProductSpecJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSpecJpaRepository;
import com.kb.wms.product.application.port.out.ProductSpecRepository;
import com.kb.wms.product.domain.entity.ProductSpec;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductSpecPersistenceAdapter implements ProductSpecRepository {

    private final ProductSpecJpaRepository productSpecJpaRepository;

    @Override
    public List<ProductSpec> findByProductId(Long productId) {
        return productSpecJpaRepository.findByProductId(productId).stream()
                .map(ProductSpecJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByProductId(Long productId) {
        return productSpecJpaRepository.existsByProductId(productId);
    }

    @Override
    public void replaceAll(Long productId, List<Long> optionValueIds) {
        productSpecJpaRepository.deleteByProductId(productId);
        productSpecJpaRepository.saveAll(optionValueIds.stream()
                .map(optionValueId -> ProductSpecJpaEntity.fromDomain(ProductSpec.assign(productId, optionValueId)))
                .toList());
    }
}

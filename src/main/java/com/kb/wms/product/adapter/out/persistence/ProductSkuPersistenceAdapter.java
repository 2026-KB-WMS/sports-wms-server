package com.kb.wms.product.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.entity.ProductSku;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductSkuPersistenceAdapter implements ProductSkuRepository {

    private final ProductSkuJpaRepository productSkuJpaRepository;

    @Override
    public ProductSku save(ProductSku productSku) {
        ProductSkuJpaEntity saved = productSkuJpaRepository.save(ProductSkuJpaEntity.fromDomain(productSku));
        return saved.toDomain();
    }

    @Override
    public Optional<ProductSku> findById(Long skuId) {
        return productSkuJpaRepository.findById(skuId).map(ProductSkuJpaEntity::toDomain);
    }

    @Override
    public List<ProductSku> findAll(Long productId) {
        return productSkuJpaRepository.findAllByFilter(productId).stream()
                .map(ProductSkuJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySkuCode(String skuCode) {
        return productSkuJpaRepository.existsBySkuCode(skuCode);
    }

    @Override
    public boolean existsByBarcode(String barcode) {
        return productSkuJpaRepository.existsByBarcode(barcode);
    }
}

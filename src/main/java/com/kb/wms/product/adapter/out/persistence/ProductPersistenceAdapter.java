package com.kb.wms.product.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.adapter.out.persistence.entity.ProductJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductJpaRepository;
import com.kb.wms.product.domain.entity.Product;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductPersistenceAdapter implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        ProductJpaEntity saved = productJpaRepository.save(ProductJpaEntity.fromDomain(product));
        return saved.toDomain();
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return productJpaRepository.findById(productId).map(ProductJpaEntity::toDomain);
    }

    @Override
    public List<Product> findAll(Long brandId, Long categoryId) {
        return productJpaRepository.findAllByFilter(brandId, categoryId).stream()
                .map(ProductJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByProductCode(String productCode) {
        return productJpaRepository.existsByProductCode(productCode);
    }
}

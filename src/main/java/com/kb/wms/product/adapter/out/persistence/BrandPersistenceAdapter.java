package com.kb.wms.product.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.common.persistence.SearchKeyword;
import com.kb.wms.product.adapter.out.persistence.entity.BrandJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.BrandJpaRepository;
import com.kb.wms.product.application.port.in.query.BrandSearchCondition;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.enums.ProductStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BrandPersistenceAdapter implements BrandRepository {

    private final BrandJpaRepository brandJpaRepository;

    @Override
    public Brand save(Brand brand) {
        BrandJpaEntity saved = brandJpaRepository.save(BrandJpaEntity.fromDomain(brand));
        return saved.toDomain();
    }

    @Override
    public Optional<Brand> findById(Long brandId) {
        return brandJpaRepository.findById(brandId).map(BrandJpaEntity::toDomain);
    }

    @Override
    public List<Brand> search(BrandSearchCondition condition) {
        return brandJpaRepository.search(
                        SearchKeyword.normalize(condition.keyword()),
                        ProductStatus.fromActiveFlag(condition.isActive()))
                .stream()
                .map(BrandJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByName(String name) {
        return brandJpaRepository.existsByName(name);
    }
}

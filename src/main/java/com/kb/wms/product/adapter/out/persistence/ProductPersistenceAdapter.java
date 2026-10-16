package com.kb.wms.product.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.persistence.PageAdapter;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.adapter.out.persistence.entity.ProductJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductJpaRepository;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.product.application.port.in.query.ProductSearchCondition;
import com.kb.wms.common.persistence.SearchKeyword;

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
    public PageResult<Product> search(ProductSearchCondition condition, PageCondition page) {
        return PageAdapter.toPageResult(productJpaRepository.search(
                        condition.brandId(),
                        condition.categoryId(),
                        SearchKeyword.normalize(condition.keyword()),
                        ProductStatus.fromActiveFlag(condition.isActive()),
                        PageAdapter.toPageable(page)),
                page, ProductJpaEntity::toDomain);
    }

    @Override
    public boolean existsByProductCode(String productCode) {
        return productJpaRepository.existsByProductCode(productCode);
    }
}

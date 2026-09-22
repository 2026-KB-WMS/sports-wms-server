package com.kb.wms.product.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.ProductUseCase;
import com.kb.wms.product.application.port.in.command.ProductRegisterCommand;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService implements ProductUseCase {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public Product registerProduct(ProductRegisterCommand command) {
        Brand brand = brandRepository.findById(command.brandId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.BRAND_NOT_FOUND));
        if (!brand.isActive()) {
            throw new BusinessException(ProductErrorCode.BRAND_INACTIVE);
        }
        Category category = categoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND));
        if (!category.isActive()) {
            throw new BusinessException(ProductErrorCode.CATEGORY_INACTIVE);
        }
        if (productRepository.existsByProductCode(command.productCode())) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_PRODUCT_CODE);
        }

        Product product = Product.register(
                command.brandId(), command.categoryId(), command.productCode(),
                command.name(), command.description());
        return productRepository.save(product);
    }

    @Override
    public List<Product> getProducts(Long brandId, Long categoryId) {
        return productRepository.findAll(brandId, categoryId);
    }

    @Override
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }
}

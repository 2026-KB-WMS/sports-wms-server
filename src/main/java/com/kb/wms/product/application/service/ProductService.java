package com.kb.wms.product.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.ProductUseCase;
import com.kb.wms.product.application.port.in.command.ProductRegisterCommand;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.domain.entity.Product;

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
        if (!brandRepository.findById(command.brandId()).isPresent()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "브랜드를 찾을 수 없습니다.");
        }
        if (!categoryRepository.findById(command.categoryId()).isPresent()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }
        if (productRepository.existsByProductCode(command.productCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 존재하는 상품 코드입니다.");
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
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }
}

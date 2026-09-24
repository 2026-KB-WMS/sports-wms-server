package com.kb.wms.product.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.BrandQueryUseCase;
import com.kb.wms.product.application.port.in.query.BrandSearchCondition;
import com.kb.wms.product.application.port.in.command.BrandRegisterCommand;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService implements BrandQueryUseCase {

    private final BrandRepository brandRepository;

    @Override
    @Transactional
    public Brand registerBrand(BrandRegisterCommand command) {
        if (brandRepository.existsByName(command.name())) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_BRAND_NAME);
        }
        Brand brand = Brand.register(command.name(), command.description());
        return brandRepository.save(brand);
    }

    @Override
    public List<Brand> getBrands(BrandSearchCondition condition) {
        return brandRepository.search(condition);
    }

    @Override
    public Brand getBrand(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.BRAND_NOT_FOUND));
    }
}

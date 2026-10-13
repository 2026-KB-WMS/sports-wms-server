package com.kb.wms.product.application.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.BrandQueryUseCase;
import com.kb.wms.product.application.port.in.query.BrandSearchCondition;
import com.kb.wms.product.application.port.in.command.BrandRegisterCommand;
import com.kb.wms.product.application.port.in.command.BrandUpdateCommand;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService implements BrandQueryUseCase {

    private static final Pattern BRAND_CODE_PATTERN = Pattern.compile("^[A-Za-z]{3}$");

    private final BrandRepository brandRepository;

    @Override
    @Transactional
    public Brand registerBrand(BrandRegisterCommand command) {
        if (brandRepository.existsByName(command.name())) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_BRAND_NAME);
        }
        String brandCode = normalizeBrandCode(command.brandCode());
        if (brandRepository.existsByBrandCode(brandCode)) {
            throw new BusinessException(ProductErrorCode.DUPLICATE_BRAND_CODE);
        }
        Brand brand = Brand.register(brandCode, command.name(), command.description());
        return brandRepository.save(brand);
    }

    @Override
    @Transactional
    public Brand updateBrand(BrandUpdateCommand command) {
        if (command.hasNoChanges()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 하나 이상 입력해주세요.");
        }
        Brand brand = brandRepository.findById(command.brandId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.BRAND_NOT_FOUND));
        if (command.brandCode() != null) {
            if (brand.getBrandCode() != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "브랜드 코드는 한 번 지정하면 바꿀 수 없습니다.");
            }
            String brandCode = normalizeBrandCode(command.brandCode());
            if (brandRepository.existsByBrandCodeAndBrandIdNot(brandCode, command.brandId())) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_BRAND_CODE);
            }
            brand.assignBrandCode(brandCode);
        }
        if (command.name() != null) {
            if (brandRepository.existsByNameAndBrandIdNot(command.name(), command.brandId())) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_BRAND_NAME);
            }
            brand.changeName(command.name());
        }
        if (command.description() != null) {
            brand.changeDescription(command.description());
        }
        return brandRepository.save(brand);
    }

    /** 영문 3자만 허용하고 대문자로 맞춘다. 웹 검증을 거치지 않은 호출도 같은 규칙을 지키게 한다. */
    private static String normalizeBrandCode(String brandCode) {
        if (brandCode == null || !BRAND_CODE_PATTERN.matcher(brandCode).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "브랜드 코드는 영문 3자여야 합니다.");
        }
        return brandCode.toUpperCase(Locale.ROOT);
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

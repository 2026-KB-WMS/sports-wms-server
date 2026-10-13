package com.kb.wms.product.application.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.ProductCodeSequenceRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.exception.ProductErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 상품 코드 발급: {@code YNX-RKT-0001} (브랜드 코드-카테고리 코드-일련번호 4자리, ADR-026).
 * 일련번호는 브랜드+카테고리 조합별 채번 행을 잠그고 올리므로 동시 등록에서도 겹치지 않는다.
 * 호출자의 트랜잭션 안에서 실행한다.
 */
@Component
@RequiredArgsConstructor
class ProductCodeIssuer {

    private final BrandRepository brandRepository;
    private final ProductCodeSequenceRepository sequenceRepository;
    private final ProductRepository productRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    String issue(Brand brand, Category category) {
        if (brand.getBrandCode() == null) {
            throw new BusinessException(ProductErrorCode.BRAND_CODE_REQUIRED);
        }
        // 조합의 첫 채번 행을 만드는 순간의 경합을 막기 위해, 같은 브랜드의 발급을 브랜드 행 잠금으로 직렬화한다.
        brandRepository.findByIdForUpdate(brand.getBrandId());
        String prefix = brand.getBrandCode() + "-" + category.getCategoryCode() + "-";
        String code;
        do {
            code = prefix + String.format("%04d", sequenceRepository.next(brand.getBrandId(), category.getCategoryId()));
            // 과거에 직접 입력된 코드와 겹치면 다음 번호로 넘어간다.
        } while (productRepository.existsByProductCode(code));
        return code;
    }
}

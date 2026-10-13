package com.kb.wms.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.ProductCodeSequenceRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class ProductCodeIssuerTest {

    @Mock
    private BrandRepository brandRepository;
    @Mock
    private ProductCodeSequenceRepository sequenceRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductCodeIssuer issuer;

    private Brand brand(String code) {
        Brand brand = Brand.register(code, "요넥스", null);
        ReflectionTestUtils.setField(brand, "brandId", 1L);
        return brand;
    }

    private Category category() {
        Category category = Category.register(null, "RKT", "라켓", 1, 0);
        ReflectionTestUtils.setField(category, "categoryId", 2L);
        return category;
    }

    @Test
    @DisplayName("브랜드 코드-카테고리 코드-일련번호 4자리로 발급하고, 브랜드 행을 먼저 잠근다")
    void issue_format() {
        Brand brand = brand("YNX");
        when(sequenceRepository.next(1L, 2L)).thenReturn(1L);
        when(productRepository.existsByProductCode("YNX-RKT-0001")).thenReturn(false);

        String code = issuer.issue(brand, category());

        assertThat(code).isEqualTo("YNX-RKT-0001");
        verify(brandRepository).findByIdForUpdate(1L);
    }

    @Test
    @DisplayName("번호가 9999를 넘으면 자릿수가 늘어난다")
    void issue_overflow() {
        when(sequenceRepository.next(1L, 2L)).thenReturn(10000L);

        assertThat(issuer.issue(brand("YNX"), category())).isEqualTo("YNX-RKT-10000");
    }

    @Test
    @DisplayName("과거에 직접 입력된 코드와 겹치면 다음 번호로 넘어간다")
    void issue_skipsExistingCode() {
        when(sequenceRepository.next(1L, 2L)).thenReturn(1L, 2L);
        when(productRepository.existsByProductCode("YNX-RKT-0001")).thenReturn(true);
        when(productRepository.existsByProductCode("YNX-RKT-0002")).thenReturn(false);

        assertThat(issuer.issue(brand("YNX"), category())).isEqualTo("YNX-RKT-0002");
    }

    @Test
    @DisplayName("브랜드 코드가 없으면 BRAND_CODE_REQUIRED 예외를 던지고 채번하지 않는다")
    void issue_brandCodeMissing() {
        Brand legacy = Brand.register("요넥스", null);
        ReflectionTestUtils.setField(legacy, "brandId", 1L);

        assertThatThrownBy(() -> issuer.issue(legacy, category()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.BRAND_CODE_REQUIRED.name());
        verify(sequenceRepository, never()).next(any(), any());
    }
}

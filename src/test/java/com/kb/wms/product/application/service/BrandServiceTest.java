package com.kb.wms.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.command.BrandRegisterCommand;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandService brandService;

    @Test
    @DisplayName("브랜드명이 중복되지 않으면 등록에 성공한다")
    void registerBrand_success() {
        when(brandRepository.existsByName("브랜드 A")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Brand result = brandService.registerBrand(new BrandRegisterCommand("브랜드 A", "설명"));

        assertThat(result.getName()).isEqualTo("브랜드 A");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("브랜드명이 중복되면 DUPLICATE_BRAND_NAME 예외를 던진다")
    void registerBrand_duplicateName_throwsBusinessException() {
        when(brandRepository.existsByName("브랜드 A")).thenReturn(true);

        assertThatThrownBy(() -> brandService.registerBrand(new BrandRegisterCommand("브랜드 A", "설명")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_BRAND_NAME.name());
        verify(brandRepository, never()).save(any());
    }

    @Test
    @DisplayName("브랜드 목록을 그대로 반환한다")
    void getBrands_returnsAll() {
        Brand brand = Brand.register("브랜드 A", "설명");
        when(brandRepository.findAll()).thenReturn(List.of(brand));

        List<Brand> result = brandService.getBrands();

        assertThat(result).containsExactly(brand);
    }

    @Test
    @DisplayName("존재하는 브랜드를 조회하면 그대로 반환한다")
    void getBrand_found_returnsBrand() {
        Brand brand = Brand.register("브랜드 A", "설명");
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        Brand result = brandService.getBrand(1L);

        assertThat(result).isEqualTo(brand);
    }

    @Test
    @DisplayName("존재하지 않는 브랜드를 조회하면 BRAND_NOT_FOUND 예외를 던진다")
    void getBrand_notFound_throwsBusinessException() {
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.getBrand(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.BRAND_NOT_FOUND.name());
    }
}

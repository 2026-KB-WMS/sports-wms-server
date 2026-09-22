package com.kb.wms.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.kb.wms.product.application.port.in.command.ProductRegisterCommand;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private final ProductRegisterCommand command =
            new ProductRegisterCommand(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");

    @Test
    @DisplayName("브랜드·카테고리가 활성이고 코드가 중복되지 않으면 등록에 성공한다")
    void registerProduct_success() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.register("브랜드 A", null)));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.register(null, "RACKET", "라켓", 1, 0)));
        when(productRepository.existsByProductCode("P-0001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.registerProduct(command);

        assertThat(result.getProductCode()).isEqualTo("P-0001");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 브랜드면 BRAND_NOT_FOUND 예외를 던진다")
    void registerProduct_brandNotFound() {
        when(brandRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.registerProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.BRAND_NOT_FOUND.name());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("비활성 브랜드면 BRAND_INACTIVE 예외를 던진다")
    void registerProduct_brandInactive() {
        Brand inactiveBrand = Brand.register("브랜드 A", null);
        inactiveBrand.deactivate();
        when(brandRepository.findById(1L)).thenReturn(Optional.of(inactiveBrand));

        assertThatThrownBy(() -> productService.registerProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.BRAND_INACTIVE.name());
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리면 CATEGORY_NOT_FOUND 예외를 던진다")
    void registerProduct_categoryNotFound() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.register("브랜드 A", null)));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.registerProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.CATEGORY_NOT_FOUND.name());
    }

    @Test
    @DisplayName("비활성 카테고리면 CATEGORY_INACTIVE 예외를 던진다")
    void registerProduct_categoryInactive() {
        Category inactiveCategory = Category.register(null, "RACKET", "라켓", 1, 0);
        inactiveCategory.deactivate();
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.register("브랜드 A", null)));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(inactiveCategory));

        assertThatThrownBy(() -> productService.registerProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.CATEGORY_INACTIVE.name());
        verify(productRepository, never()).existsByProductCode(any());
    }

    @Test
    @DisplayName("상품 코드가 중복되면 DUPLICATE_PRODUCT_CODE 예외를 던진다")
    void registerProduct_duplicateProductCode() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.register("브랜드 A", null)));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.register(null, "RACKET", "라켓", 1, 0)));
        when(productRepository.existsByProductCode("P-0001")).thenReturn(true);

        assertThatThrownBy(() -> productService.registerProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_PRODUCT_CODE.name());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("brandId·categoryId 필터를 그대로 리포지토리에 전달한다")
    void getProducts_passesFilters() {
        when(productRepository.findAll(1L, 2L)).thenReturn(List.of());

        productService.getProducts(1L, 2L);

        verify(productRepository).findAll(eq(1L), eq(2L));
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 PRODUCT_NOT_FOUND 예외를 던진다")
    void getProduct_notFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.name());
    }
}

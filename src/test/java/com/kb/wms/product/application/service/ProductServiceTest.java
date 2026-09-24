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
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.command.ProductRegisterCommand;
import com.kb.wms.product.application.port.in.command.ProductUpdateCommand;
import com.kb.wms.product.application.port.in.query.ProductSearchCondition;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductSkuRepository productSkuRepository;

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
    @DisplayName("검색 조건을 그대로 리포지토리에 전달한다")
    void getProducts_passesFilters() {
        ProductSearchCondition condition = new ProductSearchCondition(1L, 2L, "라켓", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.register("브랜드 A", null)));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(Category.register(null, "RACKET", "라켓", 1, 0)));
        when(productRepository.search(condition)).thenReturn(List.of());

        productService.getProducts(condition);

        verify(productRepository).search(eq(condition));
    }

    @Test
    @DisplayName("존재하지 않는 브랜드로 필터링하면 BRAND_NOT_FOUND 예외를 던진다")
    void getProducts_brandNotFound() {
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProducts(new ProductSearchCondition(999L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.BRAND_NOT_FOUND.name());
        verify(productRepository, never()).search(any());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 필터링하면 CATEGORY_NOT_FOUND 예외를 던진다")
    void getProducts_categoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProducts(new ProductSearchCondition(null, 999L, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.CATEGORY_NOT_FOUND.name());
        verify(productRepository, never()).search(any());
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

    @Test
    @DisplayName("변경할 필드가 없으면 VALIDATION_ERROR 예외를 던진다")
    void updateProduct_noChanges_throwsBusinessException() {
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, null, null);

        assertThatThrownBy(() -> productService.updateProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(productRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 상품을 수정하면 PRODUCT_NOT_FOUND 예외를 던진다")
    void updateProduct_productNotFound() {
        ProductUpdateCommand command = new ProductUpdateCommand(999L, "새 이름", null, null, null, null);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.name());
    }

    @Test
    @DisplayName("이름·설명만 변경하면 해당 필드만 수정되어 저장된다")
    void updateProduct_partialUpdate_success() {
        Product existing = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");
        ProductUpdateCommand command = new ProductUpdateCommand(1L, "새 이름", "새 설명", null, null, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProduct(command);

        assertThat(result.getName()).isEqualTo("새 이름");
        assertThat(result.getDescription()).isEqualTo("새 설명");
        verify(brandRepository, never()).findById(any());
        verify(categoryRepository, never()).findById(any());
        verify(productRepository).save(existing);
    }

    @Test
    @DisplayName("존재하지 않는 브랜드로 변경하면 BRAND_NOT_FOUND 예외를 던진다")
    void updateProduct_brandNotFound() {
        Product existing = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, 2L, null, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.BRAND_NOT_FOUND.name());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("비활성 브랜드로 변경하면 BRAND_INACTIVE 예외를 던진다")
    void updateProduct_brandInactive() {
        Product existing = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");
        Brand inactiveBrand = Brand.register("브랜드 B", null);
        inactiveBrand.deactivate();
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, 2L, null, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(inactiveBrand));

        assertThatThrownBy(() -> productService.updateProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.BRAND_INACTIVE.name());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 변경하면 CATEGORY_NOT_FOUND 예외를 던진다")
    void updateProduct_categoryNotFound() {
        Product existing = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, 2L, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.CATEGORY_NOT_FOUND.name());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("비활성 카테고리로 변경하면 CATEGORY_INACTIVE 예외를 던진다")
    void updateProduct_categoryInactive() {
        Product existing = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");
        Category inactiveCategory = Category.register(null, "SHOES", "신발", 1, 0);
        inactiveCategory.deactivate();
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, 2L, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(inactiveCategory));

        assertThatThrownBy(() -> productService.updateProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.CATEGORY_INACTIVE.name());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("isActive를 false로 변경하면 상품이 비활성화되어 저장된다")
    void updateProduct_deactivate_success() {
        Product existing = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, null, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProduct(command);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("상품을 비활성화하면 활성 상태인 하위 SKU도 함께 비활성화되어 저장된다")
    void updateProduct_deactivate_cascadesToSkus() {
        Product existing = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");
        ProductSku activeSku = ProductSku.register(1L, "SKU-1", null, "SKU 1", null, null, null, null, 0L);
        ProductSku inactiveSku = ProductSku.register(1L, "SKU-2", null, "SKU 2", null, null, null, null, 0L);
        inactiveSku.deactivate();
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, null, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productSkuRepository.findAll(1L)).thenReturn(List.of(activeSku, inactiveSku));

        productService.updateProduct(command);

        assertThat(activeSku.isActive()).isFalse();
        verify(productSkuRepository).save(activeSku);
        verify(productSkuRepository, never()).save(inactiveSku);
    }
}

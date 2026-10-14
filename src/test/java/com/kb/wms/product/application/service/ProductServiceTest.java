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
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.product.application.port.in.command.ProductRegisterCommand;
import com.kb.wms.product.application.port.in.command.ProductUpdateCommand;
import com.kb.wms.product.application.port.in.query.ProductSearchCondition;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.application.port.out.ProductSpecRepository;
import com.kb.wms.product.application.port.out.ProductUsagePort;
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
    @Mock
    private ProductUsagePort productUsagePort;
    @Mock
    private ProductSpecRepository productSpecRepository;
    @Mock
    private CategoryOptionResolver categoryOptionResolver;
    @Mock
    private OptionSelectionResolver optionSelectionResolver;
    @Mock
    private ProductCodeIssuer productCodeIssuer;

    @InjectMocks
    private ProductService productService;

    private final ProductRegisterCommand command =
            new ProductRegisterCommand(1L, 1L, "배드민턴 라켓 A", "초보자용", null);

    @Test
    @DisplayName("브랜드·카테고리가 활성이면 서버가 발급한 상품 코드로 등록에 성공한다")
    void registerProduct_success() {
        Brand brand = Brand.register("YNX", "브랜드 A", null);
        Category category = Category.register(null, "RKT", "라켓", 1, 0);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productCodeIssuer.issue(brand, category)).thenReturn("YNX-RKT-0001");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.registerProduct(command);

        assertThat(result.getProductCode()).isEqualTo("YNX-RKT-0001");
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
        verify(productCodeIssuer, never()).issue(any(), any());
    }

    @Test
    @DisplayName("브랜드 코드가 없어 상품 코드를 발급할 수 없으면 BRAND_CODE_REQUIRED 예외를 던지고 저장하지 않는다")
    void registerProduct_brandCodeRequired() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.register("브랜드 A", null)));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.register(null, "RKT", "라켓", 1, 0)));
        when(productCodeIssuer.issue(any(), any()))
                .thenThrow(new BusinessException(ProductErrorCode.BRAND_CODE_REQUIRED));

        assertThatThrownBy(() -> productService.registerProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.BRAND_CODE_REQUIRED.name());
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
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, null, null, null);

        assertThatThrownBy(() -> productService.updateProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(productRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 상품을 수정하면 PRODUCT_NOT_FOUND 예외를 던진다")
    void updateProduct_productNotFound() {
        ProductUpdateCommand command = new ProductUpdateCommand(999L, "새 이름", null, null, null, null, null);
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
        ProductUpdateCommand command = new ProductUpdateCommand(1L, "새 이름", "새 설명", null, null, null, null);
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
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, 2L, null, null, null);
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
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, 2L, null, null, null);
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
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, 2L, null, null);
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
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, 2L, null, null);
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
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, null, false, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProduct(command);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("재고가 남아 있으면 PRODUCT_IN_USE 409이고 상품·SKU 상태는 바뀌지 않는다")
    void updateProduct_deactivate_blockedByStock() {
        Product existing = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, null, false, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productUsagePort.hasStock(1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_IN_USE.name());

        assertThat(existing.isActive()).isTrue();
        verify(productSkuRepository, never()).findAll(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("진행 중인 입고·창고 발주·출고·지점 발주가 하나라도 있으면 PRODUCT_IN_USE 409이다")
    void updateProduct_deactivate_blockedByInProgressWork() {
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, null, false, null);
        for (int i = 0; i < 4; i++) {
            Product existing = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");
            when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(productUsagePort.hasInProgressInbounds(1L)).thenReturn(i == 0);
            when(productUsagePort.hasInProgressPurchaseOrders(1L)).thenReturn(i == 1);
            when(productUsagePort.hasInProgressOutbounds(1L)).thenReturn(i == 2);
            when(productUsagePort.hasInProgressStoreOrders(1L)).thenReturn(i == 3);

            assertThatThrownBy(() -> productService.updateProduct(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCodeName())
                    .isEqualTo(ProductErrorCode.PRODUCT_IN_USE.name());
            assertThat(existing.isActive()).isTrue();
        }
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 비활성인 상품을 다시 비활성화하는 요청은 사용 중 검사 없이 기존처럼 통과한다")
    void updateProduct_deactivate_alreadyInactiveSkipsUsageCheck() {
        Product existing = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");
        existing.deactivate();
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, null, false, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProduct(command);

        assertThat(result.isActive()).isFalse();
        verify(productUsagePort, never()).hasStock(any());
    }

    @Test
    @DisplayName("상품을 비활성화하면 활성 상태인 하위 SKU도 함께 비활성화되어 저장된다")
    void updateProduct_deactivate_cascadesToSkus() {
        Product existing = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", "초보자용");
        ProductSku activeSku = ProductSku.register(1L, "SKU-1", null, "SKU 1", null, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, null, 0L);
        ProductSku inactiveSku = ProductSku.register(1L, "SKU-2", null, "SKU 2", null, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, null, 0L);
        inactiveSku.deactivate();
        ProductUpdateCommand command = new ProductUpdateCommand(1L, null, null, null, null, false, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productSkuRepository.findAll(1L)).thenReturn(List.of(activeSku, inactiveSku));

        productService.updateProduct(command);

        assertThat(activeSku.isActive()).isFalse();
        verify(productSkuRepository).save(activeSku);
        verify(productSkuRepository, never()).save(inactiveSku);
    }


    private final AuthenticatedUser storeOwner = new AuthenticatedUser(9L, UserRole.STORE_OWNER, List.of(), List.of(1L));
    private final AuthenticatedUser warehouseManager =
            new AuthenticatedUser(8L, UserRole.WAREHOUSE_MANAGER, List.of(1L), List.of());
    private final AuthenticatedUser hqAdmin = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Test
    @DisplayName("점주의 상품 목록은 isActive 필터와 상관없이 활성 상품만 조회하고, 본사·창고 관리자는 필터를 그대로 쓴다")
    void getProducts_storeOwnerOnlyActive() {
        when(productRepository.search(new ProductSearchCondition(null, null, "라켓", true))).thenReturn(List.of());
        when(productRepository.search(new ProductSearchCondition(null, null, "라켓", false))).thenReturn(List.of());

        productService.getProducts(new ProductSearchCondition(null, null, "라켓", false), storeOwner);
        productService.getProducts(new ProductSearchCondition(null, null, "라켓", null), storeOwner);
        productService.getProducts(new ProductSearchCondition(null, null, "라켓", false), hqAdmin);
        productService.getProducts(new ProductSearchCondition(null, null, "라켓", false), warehouseManager);

        verify(productRepository, org.mockito.Mockito.times(2))
                .search(new ProductSearchCondition(null, null, "라켓", true));
        verify(productRepository, org.mockito.Mockito.times(2))
                .search(new ProductSearchCondition(null, null, "라켓", false));
    }

    @Test
    @DisplayName("점주가 비활성 상품을 조회하면 404 PRODUCT_NOT_FOUND이고, 본사·창고 관리자와 활성 상품은 조회된다")
    void getProduct_inactiveHiddenFromStoreOwner() {
        Product inactive = Product.register(1L, 1L, "P-0001", "배드민턴 라켓 A", null);
        inactive.deactivate();
        Product active = Product.register(1L, 1L, "P-0002", "배드민턴 라켓 B", null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(inactive));
        when(productRepository.findById(2L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> productService.getProduct(1L, storeOwner))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.name());
        assertThat(productService.getProduct(1L, hqAdmin)).isSameAs(inactive);
        assertThat(productService.getProduct(1L, warehouseManager)).isSameAs(inactive);
        assertThat(productService.getProduct(2L, storeOwner)).isSameAs(active);
    }

    private Product persisted(Long productId, Long categoryId) {
        Product product = Product.register(1L, categoryId, "P-0001", "배드민턴 라켓 A", "초보자용");
        org.springframework.test.util.ReflectionTestUtils.setField(product, "productId", productId);
        return product;
    }

    private com.kb.wms.product.domain.entity.OptionValue spec(Long id) {
        return com.kb.wms.product.domain.entity.OptionValue.builder().optionValueId(id).optionGroupId(3L)
                .optionCode("HEAD").value("헤드 헤비").sortOrder(1).build();
    }

    @Test
    @DisplayName("상품 등록은 카테고리의 SPEC 검증을 통과한 스펙 값을 함께 저장한다")
    void registerProduct_savesSpecs() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.register("브랜드 A", null)));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.register(null, "RACKET", "라켓", 1, 0)));
        when(optionSelectionResolver.resolve(any(), any(), any()))
                .thenReturn(List.of(spec(30L)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "productId", 5L);
            return saved;
        });

        productService.registerProduct(new ProductRegisterCommand(
                1L, 1L, "배드민턴 라켓 A", "초보자용", List.of(30L)));

        verify(productSpecRepository).replaceAll(5L, List.of(30L));
    }

    @Test
    @DisplayName("스펙 검증에 실패하면 상품을 저장하지 않는다")
    void registerProduct_specValidationFails() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.register("브랜드 A", null)));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.register(null, "RACKET", "라켓", 1, 0)));
        when(optionSelectionResolver.resolve(any(), any(), any()))
                .thenThrow(new BusinessException(ProductErrorCode.REQUIRED_OPTION_MISSING));

        assertThatThrownBy(() -> productService.registerProduct(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.REQUIRED_OPTION_MISSING.name());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("스펙 값만 보내면 스펙을 통째로 교체한다")
    void updateProduct_replacesSpecs() {
        Product existing = persisted(1L, 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(optionSelectionResolver.resolve(any(), any(), any())).thenReturn(List.of(spec(31L)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.updateProduct(new ProductUpdateCommand(1L, null, null, null, null, null, List.of(31L)));

        verify(productSpecRepository).replaceAll(1L, List.of(31L));
    }

    @Test
    @DisplayName("스펙이 있는 상품의 카테고리를 바꾸면 PRODUCT_CATEGORY_LOCKED다")
    void updateProduct_categoryLockedBySpecs() {
        Product existing = persisted(1L, 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(Category.register(null, "SHOES", "신발", 1, 0)));
        when(productSpecRepository.existsByProductId(1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(
                new ProductUpdateCommand(1L, null, null, null, 2L, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_CATEGORY_LOCKED.name());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("SKU가 있는 상품의 카테고리를 바꾸면 PRODUCT_CATEGORY_LOCKED다")
    void updateProduct_categoryLockedBySkus() {
        Product existing = persisted(1L, 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(Category.register(null, "SHOES", "신발", 1, 0)));
        when(productSkuRepository.findAll(1L)).thenReturn(List.of(ProductSku.builder().skuId(1L).build()));

        assertThatThrownBy(() -> productService.updateProduct(
                new ProductUpdateCommand(1L, null, null, null, 2L, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_CATEGORY_LOCKED.name());
    }

    @Test
    @DisplayName("스펙·SKU가 없는 상품은 카테고리를 바꿀 수 있고 새 카테고리의 SPEC 그룹으로 다시 검증하며 코드를 다시 발급한다")
    void updateProduct_categoryChangeRevalidatesSpecs() {
        Product existing = persisted(1L, 1L);
        Brand brand = Brand.register("YNX", "요넥스", null);
        Category shoes = Category.register(null, "SHO", "신발", 1, 0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(shoes));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(productCodeIssuer.issue(brand, shoes)).thenReturn("YNX-SHO-0001");
        when(optionSelectionResolver.resolve(any(), any(), any())).thenReturn(List.of());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProduct(
                new ProductUpdateCommand(1L, null, null, null, 2L, null, null));

        assertThat(result.getCategoryId()).isEqualTo(2L);
        assertThat(result.getProductCode()).isEqualTo("YNX-SHO-0001");
        verify(categoryOptionResolver).effectiveMappings(2L);
        verify(productSpecRepository).replaceAll(1L, List.of());
    }

    @Test
    @DisplayName("SKU가 없는 상품의 브랜드를 바꾸면 새 브랜드 코드로 상품 코드를 다시 발급한다")
    void updateProduct_brandChangeReissuesCode() {
        Product existing = persisted(1L, 1L);
        Brand newBrand = Brand.register("VCT", "빅터", null);
        Category category = Category.register(null, "RKT", "라켓", 1, 0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(newBrand));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productCodeIssuer.issue(newBrand, category)).thenReturn("VCT-RKT-0001");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProduct(
                new ProductUpdateCommand(1L, null, null, 2L, null, null, null));

        assertThat(result.getBrandId()).isEqualTo(2L);
        assertThat(result.getProductCode()).isEqualTo("VCT-RKT-0001");
    }

    @Test
    @DisplayName("SKU가 있는 상품의 브랜드를 바꾸면 PRODUCT_BRAND_LOCKED이고 코드를 바꾸지 않는다")
    void updateProduct_brandLockedBySkus() {
        Product existing = persisted(1L, 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(Brand.register("VCT", "빅터", null)));
        when(productSkuRepository.findAll(1L)).thenReturn(List.of(ProductSku.builder().skuId(1L).build()));

        assertThatThrownBy(() -> productService.updateProduct(
                new ProductUpdateCommand(1L, null, null, 2L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_BRAND_LOCKED.name());
        verify(productCodeIssuer, never()).issue(any(), any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("브랜드·카테고리를 바꾸지 않는 수정은 상품 코드를 다시 발급하지 않는다")
    void updateProduct_noReissueWithoutChange() {
        Product existing = persisted(1L, 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProduct(
                new ProductUpdateCommand(1L, "새 이름", null, null, null, null, null));

        assertThat(result.getProductCode()).isEqualTo("P-0001");
        verify(productCodeIssuer, never()).issue(any(), any());
    }

    @Test
    @DisplayName("같은 카테고리를 다시 보내는 요청은 잠금·스펙 검증 없이 통과한다")
    void updateProduct_sameCategoryNoop() {
        Product existing = persisted(1L, 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.register(null, "RACKET", "라켓", 1, 0)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.updateProduct(new ProductUpdateCommand(1L, null, null, null, 1L, null, null));

        verify(productSpecRepository, never()).replaceAll(any(), any());
    }

    @Test
    @DisplayName("상품 스펙 조회는 스펙이 없으면 빈 목록이다")
    void getProductSpecs_empty() {
        assertThat(productService.getProductSpecs(1L)).isEmpty();
    }
}

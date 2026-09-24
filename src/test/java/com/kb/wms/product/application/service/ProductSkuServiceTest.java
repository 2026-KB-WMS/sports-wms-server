package com.kb.wms.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;
import com.kb.wms.product.application.port.in.command.SkuOptionConnectCommand;
import com.kb.wms.product.application.port.in.query.ProductSkuSearchCondition;
import com.kb.wms.product.application.port.in.result.SkuOptionSummary;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.application.port.out.SkuOptionValueRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.product.domain.entity.SkuOptionValue;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class ProductSkuServiceTest {

    @Mock
    private ProductSkuRepository productSkuRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OptionValueRepository optionValueRepository;
    @Mock
    private SkuOptionValueRepository skuOptionValueRepository;
    @Mock
    private OptionGroupRepository optionGroupRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductSkuService productSkuService;

    private ProductSkuRegisterCommand registerCommand(Long productId, String skuCode, String barcode) {
        return new ProductSkuRegisterCommand(productId, skuCode, barcode, "라켓 A - 빨강",
                BigDecimal.valueOf(100), BigDecimal.valueOf(10000), BigDecimal.valueOf(15000),
                "EA", 10L);
    }

    private Product mockActiveProduct() {
        return Product.register(1L, 1L, "P-0001", "상품", null);
    }

    @Test
    @DisplayName("상품이 활성 상태이고 SKU 코드·바코드가 중복되지 않으면 SKU 등록에 성공한다")
    void registerSku_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockActiveProduct()));
        when(productSkuRepository.existsBySkuCode("SKU-0001")).thenReturn(false);
        when(productSkuRepository.existsByBarcode("8800000000001")).thenReturn(false);
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSku result = productSkuService.registerSku(registerCommand(1L, "SKU-0001", "8800000000001"));

        assertThat(result.getSkuCode()).isEqualTo("SKU-0001");
        assertThat(result.getProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("상품이 존재하지 않으면 PRODUCT_NOT_FOUND 예외를 던진다")
    void registerSku_productNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSkuService.registerSku(registerCommand(999L, "SKU-0001", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.name());
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("상품이 비활성 상태이면 PRODUCT_INACTIVE 예외를 던진다")
    void registerSku_productInactive() {
        Product inactiveProduct = mockActiveProduct();
        inactiveProduct.deactivate();
        when(productRepository.findById(1L)).thenReturn(Optional.of(inactiveProduct));

        assertThatThrownBy(() -> productSkuService.registerSku(registerCommand(1L, "SKU-0001", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_INACTIVE.name());
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("SKU 코드가 중복되면 DUPLICATE_SKU_CODE 예외를 던진다")
    void registerSku_duplicateSkuCode() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockActiveProduct()));
        when(productSkuRepository.existsBySkuCode("SKU-0001")).thenReturn(true);

        assertThatThrownBy(() -> productSkuService.registerSku(registerCommand(1L, "SKU-0001", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_SKU_CODE.name());
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("바코드가 중복되면 DUPLICATE_BARCODE 예외를 던진다")
    void registerSku_duplicateBarcode() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockActiveProduct()));
        when(productSkuRepository.existsBySkuCode("SKU-0001")).thenReturn(false);
        when(productSkuRepository.existsByBarcode("8800000000001")).thenReturn(true);

        assertThatThrownBy(() -> productSkuService.registerSku(registerCommand(1L, "SKU-0001", "8800000000001")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_BARCODE.name());
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("getSkus는 검색 조건을 리포지토리에 전달해 결과를 그대로 반환한다")
    void getSkus_passthrough() {
        List<ProductSku> skus = List.of(ProductSku.builder().skuId(1L).productId(1L).build());
        ProductSkuSearchCondition condition = new ProductSkuSearchCondition(1L, 2L, 3L, "SKU", true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockActiveProduct()));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(Brand.register("브랜드 A", null)));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(Category.register(null, "RACKET", "라켓", 1, 0)));
        when(productSkuRepository.search(condition)).thenReturn(skus);

        assertThat(productSkuService.getSkus(condition)).isEqualTo(skus);
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 필터링하면 PRODUCT_NOT_FOUND 예외를 던진다")
    void getSkus_productNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSkuService.getSkus(new ProductSkuSearchCondition(999L, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.name());
        verify(productSkuRepository, never()).search(any());
    }

    @Test
    @DisplayName("존재하지 않는 브랜드·카테고리로 필터링하면 각각 BRAND_NOT_FOUND, CATEGORY_NOT_FOUND 예외를 던진다")
    void getSkus_brandOrCategoryNotFound() {
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());
        when(categoryRepository.findById(998L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSkuService.getSkus(new ProductSkuSearchCondition(null, 999L, null, null, null)))
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.BRAND_NOT_FOUND.name());
        assertThatThrownBy(() -> productSkuService.getSkus(new ProductSkuSearchCondition(null, null, 998L, null, null)))
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.CATEGORY_NOT_FOUND.name());
    }

    @Test
    @DisplayName("존재하지 않는 SKU를 조회하면 SKU_NOT_FOUND 예외를 던진다")
    void getSku_notFound() {
        when(productSkuRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSkuService.getSku(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.SKU_NOT_FOUND.name());
    }

    @Test
    @DisplayName("존재하지 않는 SKU에 옵션을 연결하면 SKU_NOT_FOUND 예외를 던진다")
    void connectOptions_skuNotFound() {
        when(productSkuRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSkuService.connectOptions(new SkuOptionConnectCommand(999L, List.of(1L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.SKU_NOT_FOUND.name());
    }

    @Test
    @DisplayName("존재하지 않는 옵션 값을 연결하면 OPTION_VALUE_NOT_FOUND 예외를 던진다")
    void connectOptions_optionValueNotFound() {
        ProductSku sku = ProductSku.builder().skuId(1L).productId(1L).build();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuOptionValueRepository.findBySkuId(1L)).thenReturn(List.of());
        when(optionValueRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSkuService.connectOptions(new SkuOptionConnectCommand(1L, List.of(999L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.OPTION_VALUE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("비활성 옵션 값을 연결하면 OPTION_VALUE_INACTIVE 예외를 던진다")
    void connectOptions_optionValueInactive() {
        ProductSku sku = ProductSku.builder().skuId(1L).productId(1L).build();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuOptionValueRepository.findBySkuId(1L)).thenReturn(List.of());

        OptionValue inactiveValue = OptionValue.builder().optionValueId(10L).optionGroupId(1L).value("빨강").sortOrder(1).build();
        inactiveValue.deactivate();
        when(optionValueRepository.findById(10L)).thenReturn(Optional.of(inactiveValue));

        assertThatThrownBy(() -> productSkuService.connectOptions(new SkuOptionConnectCommand(1L, List.of(10L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.OPTION_VALUE_INACTIVE.name());
    }

    @Test
    @DisplayName("이미 연결된 옵션 값을 다시 연결하면 DUPLICATE_OPTION_VALUE 예외를 던진다")
    void connectOptions_duplicateOptionValue() {
        ProductSku sku = ProductSku.builder().skuId(1L).productId(1L).build();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuOptionValueRepository.findBySkuId(1L)).thenReturn(List.of());

        OptionValue value = OptionValue.builder().optionValueId(10L).optionGroupId(1L).value("빨강").sortOrder(1).build();
        when(optionValueRepository.findById(10L)).thenReturn(Optional.of(value));
        when(skuOptionValueRepository.existsBySkuIdAndOptionValueId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> productSkuService.connectOptions(new SkuOptionConnectCommand(1L, List.of(10L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_OPTION_VALUE.name());
    }

    @Test
    @DisplayName("같은 옵션 그룹에 속한 값을 두 개 이상 연결하려 하면 OPTION_GROUP_CONFLICT 예외를 던진다")
    void connectOptions_optionGroupConflict() {
        ProductSku sku = ProductSku.builder().skuId(1L).productId(1L).build();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuOptionValueRepository.findBySkuId(1L)).thenReturn(List.of());

        OptionValue red = OptionValue.builder().optionValueId(10L).optionGroupId(1L).value("빨강").sortOrder(1).build();
        OptionValue blue = OptionValue.builder().optionValueId(11L).optionGroupId(1L).value("파랑").sortOrder(2).build();
        when(optionValueRepository.findById(10L)).thenReturn(Optional.of(red));
        when(optionValueRepository.findById(11L)).thenReturn(Optional.of(blue));
        when(skuOptionValueRepository.existsBySkuIdAndOptionValueId(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> productSkuService.connectOptions(new SkuOptionConnectCommand(1L, List.of(10L, 11L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.OPTION_GROUP_CONFLICT.name());
    }

    @Test
    @DisplayName("서로 다른 옵션 그룹의 값들을 연결하면 모두 성공적으로 저장된다")
    void connectOptions_success() {
        ProductSku sku = ProductSku.builder().skuId(1L).productId(1L).build();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuOptionValueRepository.findBySkuId(1L)).thenReturn(List.of());

        OptionValue red = OptionValue.builder().optionValueId(10L).optionGroupId(1L).value("빨강").sortOrder(1).build();
        OptionValue g4 = OptionValue.builder().optionValueId(20L).optionGroupId(2L).value("G4").sortOrder(1).build();
        when(optionValueRepository.findById(10L)).thenReturn(Optional.of(red));
        when(optionValueRepository.findById(20L)).thenReturn(Optional.of(g4));
        when(skuOptionValueRepository.existsBySkuIdAndOptionValueId(1L, 10L)).thenReturn(false);
        when(skuOptionValueRepository.existsBySkuIdAndOptionValueId(1L, 20L)).thenReturn(false);

        productSkuService.connectOptions(new SkuOptionConnectCommand(1L, List.of(10L, 20L)));

        verify(skuOptionValueRepository).save(SkuOptionValue.connect(1L, 10L));
        verify(skuOptionValueRepository).save(SkuOptionValue.connect(1L, 20L));
    }

    @Test
    @DisplayName("getSkuOptions는 옵션 값에 연결된 옵션 그룹명까지 포함해 반환한다")
    void getSkuOptions_mapsGroupNames() {
        when(skuOptionValueRepository.findBySkuId(1L)).thenReturn(List.of(SkuOptionValue.connect(1L, 10L)));
        OptionValue red = OptionValue.builder().optionValueId(10L).optionGroupId(1L).value("빨강").sortOrder(1).build();
        when(optionValueRepository.findById(10L)).thenReturn(Optional.of(red));
        OptionGroup colorGroup = OptionGroup.builder().optionGroupId(1L).name("색상").build();
        when(optionGroupRepository.findById(1L)).thenReturn(Optional.of(colorGroup));

        List<SkuOptionSummary> result = productSkuService.getSkuOptions(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).optionGroupId()).isEqualTo(1L);
        assertThat(result.get(0).optionGroupName()).isEqualTo("색상");
        assertThat(result.get(0).optionValueId()).isEqualTo(10L);
        assertThat(result.get(0).value()).isEqualTo("빨강");
    }
}

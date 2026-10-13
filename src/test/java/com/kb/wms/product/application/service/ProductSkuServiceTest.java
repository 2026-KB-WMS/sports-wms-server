package com.kb.wms.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;
import com.kb.wms.product.application.port.in.command.ProductSkuUpdateCommand;
import com.kb.wms.product.application.port.in.command.SkuOptionConnectCommand;
import com.kb.wms.product.application.port.in.query.ProductSkuSearchCondition;
import com.kb.wms.product.application.port.in.result.SkuOptionSummary;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.application.port.out.ProductUsagePort;
import com.kb.wms.product.application.port.out.SkuOptionValueRepository;
import com.kb.wms.product.application.port.out.SkuPriceHistoryRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.CategoryOptionGroup;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.product.domain.entity.SkuOptionValue;
import com.kb.wms.product.domain.entity.SkuPriceHistory;
import com.kb.wms.product.domain.enums.OptionType;
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
    @Mock
    private ProductUsagePort productUsagePort;
    @Mock
    private SkuPriceHistoryRepository skuPriceHistoryRepository;
    @Mock
    private CategoryOptionResolver categoryOptionResolver;
    @Mock
    private OptionSelectionResolver optionSelectionResolver;

    @InjectMocks
    private ProductSkuService productSkuService;

    private ProductSkuRegisterCommand registerCommand(Long productId, String barcode) {
        return new ProductSkuRegisterCommand(productId, barcode,
                BigDecimal.valueOf(100), BigDecimal.valueOf(10000), BigDecimal.valueOf(15000),
                "EA", 10L, null);
    }

    private Product mockActiveProduct() {
        return Product.register(1L, 1L, "P-0001", "상품", null);
    }

    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    private ProductSkuUpdateCommand updateCommand(String name, String barcode, BigDecimal purchase, BigDecimal supply) {
        return new ProductSkuUpdateCommand(1L, name, barcode, null, purchase, supply, null);
    }

    private ProductSku skuWithPrices() {
        ProductSku sku = ProductSku.register(1L, "SKU-0001", "8800000000001", "라켓", null,
                BigDecimal.valueOf(10000), BigDecimal.valueOf(15000), "EA", 0L);
        // 영속화 전 도메인 객체에는 ID가 없으므로, 저장된 SKU처럼 ID를 채운다.
        org.springframework.test.util.ReflectionTestUtils.setField(sku, "skuId", 1L);
        return sku;
    }

    private ProductSkuRegisterCommand registerCommandWithUnit(String unit) {
        return new ProductSkuRegisterCommand(1L, null,
                null, BigDecimal.valueOf(10000), BigDecimal.valueOf(15000), unit, 0L, null);
    }

    @Test
    @DisplayName("허용 목록(EA, BOX, SET)의 단위는 그대로 저장하고, 생략하면 EA로 저장한다")
    void registerSku_allowedUnits() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockActiveProduct()));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (String unit : new String[] {"EA", "BOX", "SET"}) {
            assertThat(productSkuService.registerSku(registerCommandWithUnit(unit)).getUnit())
                    .as(unit).isEqualTo(unit);
        }
        assertThat(productSkuService.registerSku(registerCommandWithUnit(null)).getUnit()).isEqualTo("EA");
    }

    @Test
    @DisplayName("허용 목록 밖의 단위(소문자·한글·빈 문자열 포함)는 VALIDATION_ERROR이고 저장하지 않는다")
    void registerSku_unitOutsideAllowList() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockActiveProduct()));

        for (String unit : new String[] {"ea", "개", "PACK", "", " EA"}) {
            assertThatThrownBy(() -> productSkuService.registerSku(registerCommandWithUnit(unit)))
                    .as("unit=[" + unit + "]")
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCodeName())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        }
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("SKU 등록은 단가가 없으면 도메인에서 거부한다")
    void registerSku_priceRequired() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockActiveProduct()));
        ProductSkuRegisterCommand withoutPrice = new ProductSkuRegisterCommand(1L, null,
                null, null, BigDecimal.valueOf(15000), "EA", 0L, null);

        assertThatThrownBy(() -> productSkuService.registerSku(withoutPrice))
                .isInstanceOf(IllegalArgumentException.class);
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("단가를 바꾸면 이전·이후 단가와 처리자를 이력으로 남긴다")
    void updateSku_priceChange_recordsHistory() {
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(skuWithPrices()));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSku result = productSkuService.updateSku(
                updateCommand(null, null, BigDecimal.valueOf(12000), null), HQ);

        assertThat(result.getCurrentPurchasePrice()).isEqualByComparingTo("12000");
        assertThat(result.getCurrentSupplyPrice()).isEqualByComparingTo("15000");
        org.mockito.ArgumentCaptor<SkuPriceHistory> captor = org.mockito.ArgumentCaptor.forClass(SkuPriceHistory.class);
        verify(skuPriceHistoryRepository).save(captor.capture());
        SkuPriceHistory history = captor.getValue();
        assertThat(history.getPreviousPurchasePrice()).isEqualByComparingTo("10000");
        assertThat(history.getNewPurchasePrice()).isEqualByComparingTo("12000");
        assertThat(history.getPreviousSupplyPrice()).isEqualByComparingTo("15000");
        assertThat(history.getNewSupplyPrice()).isEqualByComparingTo("15000");
        assertThat(history.getChangedBy()).isEqualTo(1L);
    }

    @Test
    @DisplayName("단가가 아닌 필드만 바꾸면 이력을 남기지 않는다")
    void updateSku_nonPriceChange_noHistory() {
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(skuWithPrices()));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSku result = productSkuService.updateSku(updateCommand("라켓 새 이름", null, null, null), HQ);

        assertThat(result.getName()).isEqualTo("라켓 새 이름");
        verify(skuPriceHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 단가를 다시 보내면 이력을 남기지 않는다")
    void updateSku_samePrice_noHistory() {
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(skuWithPrices()));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productSkuService.updateSku(updateCommand(null, null, BigDecimal.valueOf(10000), BigDecimal.valueOf(15000)), HQ);

        verify(skuPriceHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("단가를 0으로 바꿀 수 있다")
    void updateSku_zeroPriceAllowed() {
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(skuWithPrices()));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSku result = productSkuService.updateSku(updateCommand(null, null, BigDecimal.ZERO, null), HQ);

        assertThat(result.getCurrentPurchasePrice()).isEqualByComparingTo("0");
        verify(skuPriceHistoryRepository).save(any(SkuPriceHistory.class));
    }

    @Test
    @DisplayName("비활성 SKU도 수정할 수 있다")
    void updateSku_inactiveAllowed() {
        ProductSku inactive = skuWithPrices();
        inactive.deactivate();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(inactive));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSku result = productSkuService.updateSku(updateCommand(null, null, null, BigDecimal.valueOf(1)), HQ);

        assertThat(result.isActive()).isFalse();
        assertThat(result.getCurrentSupplyPrice()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("수정할 필드가 하나도 없으면 VALIDATION_ERROR를 던진다")
    void updateSku_noChanges() {
        assertThatThrownBy(() -> productSkuService.updateSku(updateCommand(null, null, null, null), HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo("VALIDATION_ERROR");
        verify(productSkuRepository, never()).findById(any());
    }

    @Test
    @DisplayName("없는 SKU를 수정하면 SKU_NOT_FOUND를 던진다")
    void updateSku_notFound() {
        when(productSkuRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSkuService.updateSku(updateCommand("이름", null, null, null), HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.SKU_NOT_FOUND.name());
    }

    @Test
    @DisplayName("다른 SKU가 쓰는 바코드로 바꾸면 DUPLICATE_BARCODE를 던지고, 자기 바코드는 허용한다")
    void updateSku_barcodeDuplicate() {
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(skuWithPrices()));
        when(productSkuRepository.existsByBarcode("8800000000999")).thenReturn(true);

        assertThatThrownBy(() -> productSkuService.updateSku(updateCommand(null, "8800000000999", null, null), HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_BARCODE.name());
        verify(productSkuRepository, never()).save(any());

        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // 자기 자신의 바코드를 그대로 보내면 중복 조회 없이 통과한다.
        productSkuService.updateSku(updateCommand(null, "8800000000001", BigDecimal.valueOf(1), null), HQ);
        verify(productSkuRepository, never()).existsByBarcode("8800000000001");
    }

    @Test
    @DisplayName("상품이 활성 상태이고 SKU 코드·바코드가 중복되지 않으면 서버가 만든 코드로 SKU 등록에 성공한다")
    void registerSku_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockActiveProduct()));
        when(productSkuRepository.existsBySkuCode("P-0001")).thenReturn(false);
        when(productSkuRepository.existsByBarcode("8800000000001")).thenReturn(false);
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSku result = productSkuService.registerSku(registerCommand(1L, "8800000000001"));

        assertThat(result.getSkuCode()).isEqualTo("P-0001");
        assertThat(result.getName()).isEqualTo("상품");
        assertThat(result.getProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("상품이 존재하지 않으면 PRODUCT_NOT_FOUND 예외를 던진다")
    void registerSku_productNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSkuService.registerSku(registerCommand(999L, null)))
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

        assertThatThrownBy(() -> productSkuService.registerSku(registerCommand(1L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_INACTIVE.name());
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("SKU 코드가 중복되면 DUPLICATE_SKU_CODE 예외를 던진다")
    void registerSku_duplicateSkuCode() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockActiveProduct()));
        when(productSkuRepository.existsBySkuCode("P-0001")).thenReturn(true);

        assertThatThrownBy(() -> productSkuService.registerSku(registerCommand(1L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_SKU_CODE.name());
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("바코드가 중복되면 DUPLICATE_BARCODE 예외를 던진다")
    void registerSku_duplicateBarcode() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockActiveProduct()));
        when(productSkuRepository.existsBySkuCode("P-0001")).thenReturn(false);
        when(productSkuRepository.existsByBarcode("8800000000001")).thenReturn(true);

        assertThatThrownBy(() -> productSkuService.registerSku(registerCommand(1L, "8800000000001")))
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
    @DisplayName("이미 옵션이 연결된 SKU에 다시 연결하면 SKU_OPTIONS_ALREADY_CONNECTED 예외를 던진다")
    void connectOptions_alreadyConnected() {
        ProductSku sku = ProductSku.builder().skuId(1L).productId(1L).build();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuOptionValueRepository.findBySkuId(1L)).thenReturn(List.of(SkuOptionValue.connect(1L, 10L)));

        assertThatThrownBy(() -> productSkuService.connectOptions(new SkuOptionConnectCommand(1L, List.of(20L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.SKU_OPTIONS_ALREADY_CONNECTED.name());
        verify(skuOptionValueRepository, never()).save(any());
    }

    @Test
    @DisplayName("옵션이 없는 SKU에는 카테고리 검증을 통과한 옵션 값을 모두 연결한다")
    void connectOptions_success() {
        ProductSku sku = ProductSku.builder().skuId(1L).productId(1L).build();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuOptionValueRepository.findBySkuId(1L)).thenReturn(List.of());
        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithId(1L)));
        List<CategoryOptionGroup> mappings = List.of(CategoryOptionGroup.assign(1L, 1L, OptionType.SKU, 0));
        when(categoryOptionResolver.effectiveMappings(1L)).thenReturn(mappings);
        when(optionSelectionResolver.resolve(List.of(10L, 20L), mappings, OptionType.SKU))
                .thenReturn(List.of(option(10L, 1L, "RED", "빨강"), option(20L, 2L, "G4", "G4")));

        productSkuService.connectOptions(new SkuOptionConnectCommand(1L, List.of(10L, 20L)));

        verify(skuOptionValueRepository).save(SkuOptionValue.connect(1L, 10L));
        verify(skuOptionValueRepository).save(SkuOptionValue.connect(1L, 20L));
    }

    @Test
    @DisplayName("같은 상품의 다른 SKU와 옵션 조합이 같으면 DUPLICATE_SKU_OPTION_COMBINATION 예외를 던진다")
    void connectOptions_duplicateCombination() {
        ProductSku sku = ProductSku.builder().skuId(1L).productId(1L).build();
        ProductSku other = ProductSku.builder().skuId(2L).productId(1L).build();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(skuOptionValueRepository.findBySkuId(1L)).thenReturn(List.of());
        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithId(1L)));
        when(optionSelectionResolver.resolve(any(), any(), any()))
                .thenReturn(List.of(option(10L, 1L, "RED", "빨강")));
        when(productSkuRepository.findAll(1L)).thenReturn(List.of(sku, other));
        when(skuOptionValueRepository.findBySkuIdIn(List.of(2L)))
                .thenReturn(List.of(SkuOptionValue.connect(2L, 10L)));

        assertThatThrownBy(() -> productSkuService.connectOptions(new SkuOptionConnectCommand(1L, List.of(10L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_SKU_OPTION_COMBINATION.name());
        verify(skuOptionValueRepository, never()).save(any());
    }

    private Product productWithId(Long productId) {
        Product product = Product.register(1L, 1L, "P-0001", "상품", null);
        org.springframework.test.util.ReflectionTestUtils.setField(product, "productId", productId);
        return product;
    }

    private OptionValue option(Long optionValueId, Long optionGroupId, String code, String value) {
        return OptionValue.builder().optionValueId(optionValueId).optionGroupId(optionGroupId)
                .optionCode(code).value(value).sortOrder(1).build();
    }

    private ProductSkuRegisterCommand autoRegisterCommand(List<Long> optionValueIds) {
        return new ProductSkuRegisterCommand(1L, null, null,
                BigDecimal.valueOf(10000), BigDecimal.valueOf(15000), "EA", 0L, optionValueIds);
    }

    @Test
    @DisplayName("SKU 코드·이름은 상품 코드와 옵션 코드, 상품명과 옵션 값으로 만들고 옵션도 함께 저장한다")
    void registerSku_autoCodeAndName() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithId(1L)));
        when(optionSelectionResolver.resolve(any(), any(), any()))
                .thenReturn(List.of(option(10L, 1L, "RED", "빨강"), option(20L, 2L, "G4", "G4")));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> {
            ProductSku saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "skuId", 7L);
            return saved;
        });

        ProductSku result = productSkuService.registerSku(autoRegisterCommand(List.of(10L, 20L)));

        assertThat(result.getSkuCode()).isEqualTo("P-0001-RED-G4");
        assertThat(result.getName()).isEqualTo("상품 (빨강/G4)");
        verify(skuOptionValueRepository).save(SkuOptionValue.connect(7L, 10L));
        verify(skuOptionValueRepository).save(SkuOptionValue.connect(7L, 20L));
    }

    @Test
    @DisplayName("옵션이 없는 카테고리의 SKU는 코드가 상품 코드, 이름이 상품명 그대로다")
    void registerSku_autoCodeWithoutOptions() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithId(1L)));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSku result = productSkuService.registerSku(autoRegisterCommand(null));

        assertThat(result.getSkuCode()).isEqualTo("P-0001");
        assertThat(result.getName()).isEqualTo("상품");
    }

    @Test
    @DisplayName("코드가 없는 옵션 값으로는 SKU 코드를 만들 수 없어 VALIDATION_ERROR다")
    void registerSku_optionWithoutCode() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithId(1L)));
        when(optionSelectionResolver.resolve(any(), any(), any()))
                .thenReturn(List.of(option(10L, 1L, null, "빨강")));

        assertThatThrownBy(() -> productSkuService.registerSku(autoRegisterCommand(List.of(10L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("만들어진 SKU 코드가 50자를 넘으면 VALIDATION_ERROR다")
    void registerSku_generatedCodeTooLong() {
        Product product = Product.register(1L, 1L, "P".repeat(49), "상품", null);
        org.springframework.test.util.ReflectionTestUtils.setField(product, "productId", 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(optionSelectionResolver.resolve(any(), any(), any()))
                .thenReturn(List.of(option(10L, 1L, "RED", "빨강")));

        assertThatThrownBy(() -> productSkuService.registerSku(autoRegisterCommand(List.of(10L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("같은 상품에 같은 옵션 조합의 SKU가 있으면 DUPLICATE_SKU_OPTION_COMBINATION이고 저장하지 않는다")
    void registerSku_duplicateCombination() {
        ProductSku existing = ProductSku.builder().skuId(5L).productId(1L).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithId(1L)));
        when(optionSelectionResolver.resolve(any(), any(), any()))
                .thenReturn(List.of(option(10L, 1L, "RED", "빨강"), option(20L, 2L, "G4", "G4")));
        when(productSkuRepository.findAll(1L)).thenReturn(List.of(existing));
        when(skuOptionValueRepository.findBySkuIdIn(List.of(5L))).thenReturn(
                List.of(SkuOptionValue.connect(5L, 20L), SkuOptionValue.connect(5L, 10L)));

        assertThatThrownBy(() -> productSkuService.registerSku(autoRegisterCommand(List.of(10L, 20L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_SKU_OPTION_COMBINATION.name());
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("옵션이 일부만 겹치는 SKU는 다른 조합이므로 등록할 수 있다")
    void registerSku_partiallyOverlappingCombination() {
        ProductSku existing = ProductSku.builder().skuId(5L).productId(1L).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithId(1L)));
        when(optionSelectionResolver.resolve(any(), any(), any()))
                .thenReturn(List.of(option(10L, 1L, "RED", "빨강"), option(21L, 2L, "G5", "G5")));
        when(productSkuRepository.findAll(1L)).thenReturn(List.of(existing));
        when(skuOptionValueRepository.findBySkuIdIn(List.of(5L))).thenReturn(
                List.of(SkuOptionValue.connect(5L, 10L), SkuOptionValue.connect(5L, 20L)));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSku result = productSkuService.registerSku(autoRegisterCommand(List.of(10L, 21L)));

        assertThat(result.getSkuCode()).isEqualTo("P-0001-RED-G5");
    }

    @Test
    @DisplayName("만들어진 SKU 코드가 이미 있으면 DUPLICATE_SKU_CODE다")
    void registerSku_generatedCodeDuplicate() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(productWithId(1L)));
        when(optionSelectionResolver.resolve(any(), any(), any()))
                .thenReturn(List.of(option(10L, 1L, "RED", "빨강")));
        when(productSkuRepository.existsBySkuCode("P-0001-RED")).thenReturn(true);

        assertThatThrownBy(() -> productSkuService.registerSku(autoRegisterCommand(List.of(10L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_SKU_CODE.name());
    }

    @Test
    @DisplayName("getSkuOptions는 옵션 값에 연결된 옵션 그룹명까지 포함해 반환한다")
    void getSkuOptions_mapsGroupNames() {
        when(skuOptionValueRepository.findBySkuIdIn(List.of(1L))).thenReturn(List.of(SkuOptionValue.connect(1L, 10L)));
        OptionValue red = OptionValue.builder().optionValueId(10L).optionGroupId(1L).value("빨강").sortOrder(1).build();
        when(optionValueRepository.findAllByIds(List.of(10L))).thenReturn(List.of(red));
        OptionGroup colorGroup = OptionGroup.builder().optionGroupId(1L).name("색상").build();
        when(optionGroupRepository.findAllByIds(List.of(1L))).thenReturn(List.of(colorGroup));

        List<SkuOptionSummary> result = productSkuService.getSkuOptions(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).optionGroupId()).isEqualTo(1L);
        assertThat(result.get(0).optionGroupName()).isEqualTo("색상");
        assertThat(result.get(0).optionValueId()).isEqualTo(10L);
        assertThat(result.get(0).value()).isEqualTo("빨강");
    }

    @Test
    @DisplayName("여러 SKU의 옵션은 SKU 수와 관계없이 연결·옵션 값·옵션 그룹을 한 번씩만 조회해 SKU별로 묶어 반환한다")
    void getSkuOptionsBySkuIds_batchQueries() {
        when(skuOptionValueRepository.findBySkuIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(
                SkuOptionValue.connect(1L, 10L), SkuOptionValue.connect(1L, 20L),
                SkuOptionValue.connect(2L, 10L)));
        OptionValue red = OptionValue.builder().optionValueId(10L).optionGroupId(1L).value("빨강").sortOrder(1).build();
        OptionValue g4 = OptionValue.builder().optionValueId(20L).optionGroupId(2L).value("G4").sortOrder(1).build();
        when(optionValueRepository.findAllByIds(anyCollection())).thenReturn(List.of(red, g4));
        when(optionGroupRepository.findAllByIds(anyCollection())).thenReturn(List.of(
                OptionGroup.builder().optionGroupId(1L).name("색상").build(),
                OptionGroup.builder().optionGroupId(2L).name("그립").build()));

        java.util.Map<Long, List<SkuOptionSummary>> result =
                productSkuService.getSkuOptionsBySkuIds(List.of(1L, 2L, 3L));

        assertThat(result.get(1L)).extracting(SkuOptionSummary::value).containsExactly("빨강", "G4");
        assertThat(result.get(2L)).extracting(SkuOptionSummary::optionGroupName).containsExactly("색상");
        assertThat(result).doesNotContainKey(3L);
        verify(skuOptionValueRepository).findBySkuIdIn(List.of(1L, 2L, 3L));
        verify(optionValueRepository, times(1)).findAllByIds(anyCollection());
        verify(optionGroupRepository, times(1)).findAllByIds(anyCollection());
    }

    private ProductSku sku() {
        return ProductSku.register(1L, "SKU-0001", null, "라켓", null, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, null, 0L);
    }

    @Test
    @DisplayName("SKU를 비활성화하면 상태가 바뀌어 저장된다")
    void changeSkuStatus_deactivate() {
        ProductSku sku = sku();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSku result = productSkuService.changeSkuStatus(1L, false);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("재고가 남아 있거나 진행 중인 업무가 있는 SKU는 비활성화할 수 없다")
    void changeSkuStatus_deactivate_inUse() {
        ProductSku sku = sku();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(productUsagePort.isSkuInUse(1L)).thenReturn(true);

        assertThatThrownBy(() -> productSkuService.changeSkuStatus(1L, false))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.SKU_IN_USE.name());
        assertThat(sku.isActive()).isTrue();
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 비활성인 SKU를 다시 비활성화하면 사용 중 검사 없이 그대로 통과한다")
    void changeSkuStatus_deactivate_alreadyInactive() {
        ProductSku sku = sku();
        sku.deactivate();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(productSkuService.changeSkuStatus(1L, false).isActive()).isFalse();
        verify(productUsagePort, never()).isSkuInUse(any());
    }

    @Test
    @DisplayName("상품이 활성이면 비활성 SKU를 다시 활성화할 수 있다")
    void changeSkuStatus_activate() {
        ProductSku sku = sku();
        sku.deactivate();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockActiveProduct()));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(productSkuService.changeSkuStatus(1L, true).isActive()).isTrue();
    }

    @Test
    @DisplayName("상품이 비활성이면 SKU 활성화는 PRODUCT_INACTIVE 예외를 던진다")
    void changeSkuStatus_activate_productInactive() {
        ProductSku sku = sku();
        sku.deactivate();
        Product inactiveProduct = mockActiveProduct();
        inactiveProduct.deactivate();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(productRepository.findById(1L)).thenReturn(Optional.of(inactiveProduct));

        assertThatThrownBy(() -> productSkuService.changeSkuStatus(1L, true))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_INACTIVE.name());
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 SKU의 상태를 바꾸면 SKU_NOT_FOUND 예외를 던진다")
    void changeSkuStatus_notFound() {
        when(productSkuRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSkuService.changeSkuStatus(999L, false))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.SKU_NOT_FOUND.name());
    }


    private final AuthenticatedUser storeOwner = new AuthenticatedUser(9L, UserRole.STORE_OWNER, List.of(), List.of(1L));
    private final AuthenticatedUser hqAdmin = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Test
    @DisplayName("점주의 SKU 목록은 isActive 필터와 상관없이 활성 SKU만 조회하고, 본사는 필터를 그대로 쓴다")
    void getSkus_storeOwnerOnlyActive() {
        when(productSkuRepository.search(new ProductSkuSearchCondition(null, null, null, "SKU", true)))
                .thenReturn(List.of());
        when(productSkuRepository.search(new ProductSkuSearchCondition(null, null, null, "SKU", false)))
                .thenReturn(List.of());

        productSkuService.getSkus(new ProductSkuSearchCondition(null, null, null, "SKU", false), storeOwner);
        productSkuService.getSkus(new ProductSkuSearchCondition(null, null, null, "SKU", false), hqAdmin);

        verify(productSkuRepository).search(new ProductSkuSearchCondition(null, null, null, "SKU", true));
        verify(productSkuRepository).search(new ProductSkuSearchCondition(null, null, null, "SKU", false));
    }

    @Test
    @DisplayName("점주가 비활성 SKU를 조회하면 404 SKU_NOT_FOUND이고, 본사와 활성 SKU는 조회된다")
    void getSku_inactiveHiddenFromStoreOwner() {
        ProductSku inactive = ProductSku.register(1L, "SKU-0001", "8800000000001", "라켓 A - 빨강",
                java.math.BigDecimal.TEN, java.math.BigDecimal.valueOf(10000), java.math.BigDecimal.valueOf(15000), "EA", 10L);
        ProductSku active = ProductSku.register(1L, "SKU-0002", "8800000000002", "라켓 A - 파랑",
                java.math.BigDecimal.TEN, java.math.BigDecimal.valueOf(10000), java.math.BigDecimal.valueOf(15000), "EA", 10L);
        inactive.deactivate();
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(inactive));
        when(productSkuRepository.findById(2L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> productSkuService.getSku(1L, storeOwner))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.SKU_NOT_FOUND.name());
        assertThat(productSkuService.getSku(1L, hqAdmin)).isSameAs(inactive);
        assertThat(productSkuService.getSku(2L, storeOwner)).isSameAs(active);
    }
}

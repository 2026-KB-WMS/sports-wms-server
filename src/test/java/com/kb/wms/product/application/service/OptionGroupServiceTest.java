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
import com.kb.wms.product.application.port.in.command.OptionGroupRegisterCommand;
import com.kb.wms.product.application.port.in.result.ProductOptionGroupSummary;
import com.kb.wms.product.application.port.out.OptionGroupRepository;
import com.kb.wms.product.application.port.out.OptionValueRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.application.port.out.SkuOptionValueRepository;
import com.kb.wms.product.domain.entity.OptionGroup;
import com.kb.wms.product.domain.entity.OptionValue;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.product.domain.entity.SkuOptionValue;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class OptionGroupServiceTest {

    @Mock
    private OptionGroupRepository optionGroupRepository;
    @Mock
    private OptionValueRepository optionValueRepository;
    @Mock
    private SkuOptionValueRepository skuOptionValueRepository;
    @Mock
    private ProductSkuRepository productSkuRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OptionGroupService optionGroupService;

    @Test
    @DisplayName("옵션 그룹명이 중복되지 않으면 등록에 성공한다")
    void registerOptionGroup_success() {
        when(optionGroupRepository.existsByName("색상")).thenReturn(false);
        when(optionGroupRepository.save(any(OptionGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OptionGroup result = optionGroupService.registerOptionGroup(new OptionGroupRegisterCommand("색상"));

        assertThat(result.getName()).isEqualTo("색상");
    }

    @Test
    @DisplayName("옵션 그룹명이 중복되면 DUPLICATE_OPTION_GROUP_NAME 예외를 던진다")
    void registerOptionGroup_duplicateName() {
        when(optionGroupRepository.existsByName("색상")).thenReturn(true);

        assertThatThrownBy(() -> optionGroupService.registerOptionGroup(new OptionGroupRegisterCommand("색상")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.DUPLICATE_OPTION_GROUP_NAME.name());
        verify(optionGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 PRODUCT_NOT_FOUND 예외를 던진다")
    void getOptionGroupsByProduct_productNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> optionGroupService.getOptionGroupsByProduct(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.name());
    }

    @Test
    @DisplayName("상품의 SKU들에 연결된 옵션 값을 옵션 그룹 단위로 묶어 optionGroupId·sortOrder 오름차순으로 반환한다")
    void getOptionGroupsByProduct_groupsAndSortsValues() {
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct()));

        ProductSku sku1 = ProductSku.builder().skuId(1L).productId(productId).build();
        ProductSku sku2 = ProductSku.builder().skuId(2L).productId(productId).build();
        when(productSkuRepository.findAll(productId)).thenReturn(List.of(sku1, sku2));

        when(skuOptionValueRepository.findBySkuId(1L)).thenReturn(List.of(
                SkuOptionValue.connect(1L, 10L), // 색상=빨강
                SkuOptionValue.connect(1L, 20L)  // 그립사이즈=G4
        ));
        when(skuOptionValueRepository.findBySkuId(2L)).thenReturn(List.of(
                SkuOptionValue.connect(2L, 11L) // 색상=파랑
        ));

        OptionValue red = OptionValue.builder().optionValueId(10L).optionGroupId(1L).value("빨강").sortOrder(1).build();
        OptionValue blue = OptionValue.builder().optionValueId(11L).optionGroupId(1L).value("파랑").sortOrder(2).build();
        OptionValue g4 = OptionValue.builder().optionValueId(20L).optionGroupId(2L).value("G4").sortOrder(1).build();
        when(optionValueRepository.findById(10L)).thenReturn(Optional.of(red));
        when(optionValueRepository.findById(11L)).thenReturn(Optional.of(blue));
        when(optionValueRepository.findById(20L)).thenReturn(Optional.of(g4));

        OptionGroup colorGroup = OptionGroup.builder().optionGroupId(1L).name("색상").build();
        OptionGroup sizeGroup = OptionGroup.builder().optionGroupId(2L).name("그립 사이즈").build();
        when(optionGroupRepository.findById(1L)).thenReturn(Optional.of(colorGroup));
        when(optionGroupRepository.findById(2L)).thenReturn(Optional.of(sizeGroup));

        List<ProductOptionGroupSummary> result = optionGroupService.getOptionGroupsByProduct(productId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).optionGroupId()).isEqualTo(1L);
        assertThat(result.get(0).values()).extracting("value").containsExactly("빨강", "파랑");
        assertThat(result.get(1).optionGroupId()).isEqualTo(2L);
        assertThat(result.get(1).values()).extracting("value").containsExactly("G4");
    }

    private Product mockProduct() {
        return Product.register(1L, 1L, "P-0001", "상품", null);
    }
}

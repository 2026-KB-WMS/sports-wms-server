package com.kb.wms.product.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.product.application.port.in.BrandQueryUseCase;
import com.kb.wms.product.application.port.in.CategoryUseCase;
import com.kb.wms.product.application.port.in.ProductSkuUseCase;
import com.kb.wms.product.application.port.in.ProductUseCase;
import com.kb.wms.product.application.port.in.command.ProductSkuRegisterCommand;
import com.kb.wms.product.application.port.in.command.SkuOptionConnectCommand;
import com.kb.wms.product.application.port.in.query.ProductSkuSearchCondition;
import com.kb.wms.product.application.port.in.result.SkuOptionSummary;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.product.exception.ProductErrorCode;

@WebMvcTest(ProductSkuController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductSkuControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductSkuUseCase productSkuUseCase;
    @MockitoBean
    private ProductUseCase productUseCase;
    @MockitoBean
    private BrandQueryUseCase brandQueryUseCase;
    @MockitoBean
    private CategoryUseCase categoryUseCase;

    private record SkuRequest(Long productId, String skuCode, String barcode, String skuName, BigDecimal weight,
                               BigDecimal currentPurchasePrice, BigDecimal currentSupplyPrice, String unit,
                               Long safetyStockQuantity) {
    }

    private record ConnectRequest(List<Long> optionValueIds) {
    }

    private ProductSku mockSku() {
        return ProductSku.register(1L, "SKU-0001", "8800000000001", "라켓 A - 빨강",
                BigDecimal.TEN, BigDecimal.valueOf(10000), BigDecimal.valueOf(15000), "EA", 10L);
    }

    @Test
    @DisplayName("SKU 등록에 성공하면 201과 등록된 SKU를 반환한다")
    void registerSku_success() throws Exception {
        when(productSkuUseCase.registerSku(any(ProductSkuRegisterCommand.class))).thenReturn(mockSku());

        mockMvc.perform(post("/api/v1/products/skus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SkuRequest(
                                1L, "SKU-0001", "8800000000001", "라켓 A - 빨강", BigDecimal.TEN,
                                BigDecimal.valueOf(10000), BigDecimal.valueOf(15000), "EA", 10L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.skuCode").value("SKU-0001"))
                .andExpect(jsonPath("$.data.optionValues").isEmpty());
    }

    @Test
    @DisplayName("비활성 상품에 SKU를 등록하면 409 PRODUCT_INACTIVE를 반환한다")
    void registerSku_productInactive() throws Exception {
        when(productSkuUseCase.registerSku(any(ProductSkuRegisterCommand.class)))
                .thenThrow(new BusinessException(ProductErrorCode.PRODUCT_INACTIVE));

        mockMvc.perform(post("/api/v1/products/skus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SkuRequest(
                                1L, "SKU-0001", null, "라켓 A - 빨강", null, null, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_INACTIVE"));
    }

    @Test
    @DisplayName("SKU 목록을 조회하면 200과 상품명·옵션이 포함된 목록을 반환한다")
    void getSkus_success() throws Exception {
        ProductSku sku = mockSku();
        when(productSkuUseCase.getSkus(new ProductSkuSearchCondition(1L, 2L, 3L, "SKU", true)))
                .thenReturn(List.of(sku));
        when(productUseCase.getProduct(sku.getProductId())).thenReturn(Product.register(1L, 1L, "P-0001", "라켓 A", null));
        when(productSkuUseCase.getSkuOptions(sku.getSkuId())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/products/skus")
                        .param("productId", "1").param("brandId", "2").param("categoryId", "3")
                        .param("keyword", "SKU").param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].skuCode").value("SKU-0001"))
                .andExpect(jsonPath("$.data.items[0].productName").value("라켓 A"));
    }

    @Test
    @DisplayName("SKU를 단건 조회하면 200과 상품·브랜드·카테고리 정보를 포함해 반환한다")
    void getSku_success() throws Exception {
        ProductSku sku = mockSku();
        Product product = Product.register(1L, 2L, "P-0001", "라켓 A", null);
        when(productSkuUseCase.getSku(1L)).thenReturn(sku);
        when(productUseCase.getProduct(sku.getProductId())).thenReturn(product);
        when(brandQueryUseCase.getBrand(1L)).thenReturn(Brand.register("브랜드 A", null));
        when(categoryUseCase.getCategory(2L)).thenReturn(Category.register(null, "CAT-002", "라켓", 1, 0));
        when(productSkuUseCase.getSkuOptions(sku.getSkuId())).thenReturn(
                List.of(new SkuOptionSummary(1L, "색상", 10L, "빨강")));

        mockMvc.perform(get("/api/v1/products/skus/{skuId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skuCode").value("SKU-0001"))
                .andExpect(jsonPath("$.data.brandName").value("브랜드 A"))
                .andExpect(jsonPath("$.data.categoryName").value("라켓"))
                .andExpect(jsonPath("$.data.optionValues[0].value").value("빨강"));
    }

    @Test
    @DisplayName("존재하지 않는 SKU를 조회하면 404 SKU_NOT_FOUND를 반환한다")
    void getSku_notFound() throws Exception {
        when(productSkuUseCase.getSku(999L)).thenThrow(new BusinessException(ProductErrorCode.SKU_NOT_FOUND));

        mockMvc.perform(get("/api/v1/products/skus/{skuId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SKU_NOT_FOUND"));
    }

    @Test
    @DisplayName("옵션 연결에 성공하면 201과 연결된 옵션 목록을 반환한다")
    void connectOptions_success() throws Exception {
        when(productSkuUseCase.getSkuOptions(1L)).thenReturn(List.of(new SkuOptionSummary(1L, "색상", 10L, "빨강")));

        mockMvc.perform(post("/api/v1/products/skus/{skuId}/options", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConnectRequest(List.of(10L)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.skuId").value(1))
                .andExpect(jsonPath("$.data.optionValues[0].value").value("빨강"));
    }

    @Test
    @DisplayName("같은 옵션 그룹의 값을 중복 연결하면 409 OPTION_GROUP_CONFLICT를 반환한다")
    void connectOptions_optionGroupConflict() throws Exception {
        doThrow(new BusinessException(ProductErrorCode.OPTION_GROUP_CONFLICT))
                .when(productSkuUseCase).connectOptions(any(SkuOptionConnectCommand.class));

        mockMvc.perform(post("/api/v1/products/skus/{skuId}/options", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConnectRequest(List.of(10L, 11L)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("OPTION_GROUP_CONFLICT"));
    }

    @Test
    @DisplayName("연결할 옵션 값이 비어있으면 400 VALIDATION_ERROR를 반환한다")
    void connectOptions_validationError() throws Exception {
        mockMvc.perform(post("/api/v1/products/skus/{skuId}/options", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConnectRequest(List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}

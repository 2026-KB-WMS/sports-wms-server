package com.kb.wms.product.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.kb.wms.product.application.port.in.ProductUseCase;
import com.kb.wms.product.application.port.in.command.ProductRegisterCommand;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.exception.ProductErrorCode;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductUseCase productUseCase;
    @MockitoBean
    private BrandQueryUseCase brandQueryUseCase;
    @MockitoBean
    private CategoryUseCase categoryUseCase;

    private record TestRequest(Long brandId, Long categoryId, String productCode, String productName,
                                String description) {
    }

    @Test
    @DisplayName("상품 등록에 성공하면 201과 등록된 상품을 반환한다")
    void registerProduct_success() throws Exception {
        Product product = Product.register(1L, 1L, "P-0001", "라켓 A", "설명");
        when(productUseCase.registerProduct(any(ProductRegisterCommand.class))).thenReturn(product);
        when(brandQueryUseCase.getBrand(1L)).thenReturn(Brand.register("브랜드 A", null));
        when(categoryUseCase.getCategory(1L)).thenReturn(Category.register(null, "CAT-001", "라켓", 1, 0));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestRequest(1L, 1L, "P-0001", "라켓 A", "설명"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productCode").value("P-0001"))
                .andExpect(jsonPath("$.data.brandName").value("브랜드 A"))
                .andExpect(jsonPath("$.data.categoryName").value("라켓"));
    }

    @Test
    @DisplayName("존재하지 않는 브랜드로 상품을 등록하면 404 BRAND_NOT_FOUND를 반환한다")
    void registerProduct_brandNotFound() throws Exception {
        when(productUseCase.registerProduct(any(ProductRegisterCommand.class)))
                .thenThrow(new BusinessException(ProductErrorCode.BRAND_NOT_FOUND));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TestRequest(999L, 1L, "P-0001", "라켓 A", "설명"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("BRAND_NOT_FOUND"));
    }

    @Test
    @DisplayName("상품 목록을 조회하면 200과 브랜드·카테고리 필터가 그대로 전달된다")
    void getProducts_success() throws Exception {
        Product product = Product.register(1L, 2L, "P-0001", "라켓 A", "설명");
        when(productUseCase.getProducts(eq(1L), eq(2L))).thenReturn(List.of(product));
        when(brandQueryUseCase.getBrand(1L)).thenReturn(Brand.register("브랜드 A", null));
        when(categoryUseCase.getCategory(2L)).thenReturn(Category.register(null, "CAT-002", "가방", 1, 0));

        mockMvc.perform(get("/api/v1/products").param("brandId", "1").param("categoryId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productCode").value("P-0001"));
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 404 PRODUCT_NOT_FOUND를 반환한다")
    void getProduct_notFound() throws Exception {
        when(productUseCase.getProduct(999L)).thenThrow(new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/products/{productId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("PRODUCT_NOT_FOUND"));
    }
}

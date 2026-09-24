package com.kb.wms.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.product.application.port.in.query.BrandSearchCondition;
import com.kb.wms.product.application.port.in.query.CategorySearchCondition;
import com.kb.wms.product.application.port.in.query.ProductSearchCondition;
import com.kb.wms.product.application.port.in.query.ProductSkuSearchCondition;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.CategoryRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.application.port.out.ProductSkuRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Category;
import com.kb.wms.product.domain.entity.Product;
import com.kb.wms.product.domain.entity.ProductSku;
import com.kb.wms.warehouse.application.port.in.query.WarehouseSearchCondition;
import com.kb.wms.warehouse.application.port.in.query.WarehouseSectionSearchCondition;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;

/**
 * 목록 API 검색 쿼리(keyword·isActive·연관 필터)가 실제 DB에서 조건대로 동작하는지 검증한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        // 다른 통합 테스트 컨텍스트(같은 인메모리 DB를 공유하며 create-drop)와 스키마가 섞이지 않도록 전용 DB를 쓴다.
        // Category.depth가 MySQL 전용 TINYINT(columnDefinition)라 MySQL 모드가 필요하다.
        "spring.datasource.url=jdbc:h2:mem:list_search;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class ListSearchQueryIntegrationTest {

    @Autowired private BrandRepository brandRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductSkuRepository productSkuRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private WarehouseSectionRepository warehouseSectionRepository;

    private Brand brandA;
    private Brand brandB;
    private Category racket;
    private Category shoes;
    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        brandA = brandRepository.save(Brand.register("요넥스", null));
        Brand inactive = Brand.register("빅터", null);
        inactive.deactivate();
        brandB = brandRepository.save(inactive);

        racket = categoryRepository.save(Category.register(null, "RACKET", "라켓", 1, 2));
        Category child = Category.register(racket.getCategoryId(), "RACKET-PRO", "프로 라켓", 2, 1);
        categoryRepository.save(child);
        shoes = categoryRepository.save(Category.register(null, "SHOES", "신발", 1, 0));

        productA = productRepository.save(Product.register(brandA.getBrandId(), racket.getCategoryId(),
                "P-0001", "아스트록스 라켓", null));
        Product inactiveProduct = Product.register(brandB.getBrandId(), shoes.getCategoryId(), "P-0002", "배드민턴 신발", null);
        inactiveProduct.deactivate();
        productB = productRepository.save(inactiveProduct);

        productSkuRepository.save(ProductSku.register(productA.getProductId(), "SKU-RED", "8800000000001", "라켓 빨강",
                null, null, null, "EA", 0L));
        ProductSku inactiveSku = ProductSku.register(productB.getProductId(), "SKU-SHOE-270", null, "신발 270",
                null, null, null, "EA", 0L);
        inactiveSku.deactivate();
        productSkuRepository.save(inactiveSku);
    }

    @Test
    @DisplayName("브랜드: keyword는 대소문자 무시 부분 일치, isActive는 상태 필터")
    void brands() {
        assertThat(brandRepository.search(new BrandSearchCondition(null, null))).hasSize(2);
        assertThat(brandRepository.search(new BrandSearchCondition("요넥", null)))
                .extracting(Brand::getName).containsExactly("요넥스");
        assertThat(brandRepository.search(new BrandSearchCondition(null, false)))
                .extracting(Brand::getName).containsExactly("빅터");
        assertThat(brandRepository.search(new BrandSearchCondition("  ", true)))
                .extracting(Brand::getName).containsExactly("요넥스");
    }

    @Test
    @DisplayName("카테고리: 상위 카테고리·깊이·keyword(이름 또는 코드)로 필터링하고 sortOrder 순으로 정렬한다")
    void categories() {
        assertThat(categoryRepository.search(new CategorySearchCondition(null, 1, null, null)))
                .extracting(Category::getCategoryCode).containsExactly("SHOES", "RACKET");
        assertThat(categoryRepository.search(new CategorySearchCondition(racket.getCategoryId(), null, null, null)))
                .extracting(Category::getCategoryCode).containsExactly("RACKET-PRO");
        assertThat(categoryRepository.search(new CategorySearchCondition(null, null, "pro", null)))
                .extracting(Category::getCategoryCode).containsExactly("RACKET-PRO");
    }

    @Test
    @DisplayName("상품: 브랜드·카테고리·keyword(이름 또는 코드)·isActive를 조합해 필터링한다")
    void products() {
        assertThat(productRepository.search(new ProductSearchCondition(brandA.getBrandId(), null, null, null)))
                .extracting(Product::getProductCode).containsExactly("P-0001");
        assertThat(productRepository.search(new ProductSearchCondition(null, shoes.getCategoryId(), null, null)))
                .extracting(Product::getProductCode).containsExactly("P-0002");
        assertThat(productRepository.search(new ProductSearchCondition(null, null, "p-0002", null)))
                .extracting(Product::getProductCode).containsExactly("P-0002");
        assertThat(productRepository.search(new ProductSearchCondition(null, null, null, true)))
                .extracting(Product::getProductCode).containsExactly("P-0001");
    }

    @Test
    @DisplayName("SKU: 소속 상품의 브랜드·카테고리와 keyword(코드·바코드·이름)·isActive로 필터링한다")
    void skus() {
        assertThat(productSkuRepository.search(new ProductSkuSearchCondition(null, brandA.getBrandId(), null, null, null)))
                .extracting(ProductSku::getSkuCode).containsExactly("SKU-RED");
        assertThat(productSkuRepository.search(new ProductSkuSearchCondition(null, null, shoes.getCategoryId(), null, null)))
                .extracting(ProductSku::getSkuCode).containsExactly("SKU-SHOE-270");
        assertThat(productSkuRepository.search(new ProductSkuSearchCondition(null, null, null, "8800000000001", null)))
                .extracting(ProductSku::getSkuCode).containsExactly("SKU-RED");
        assertThat(productSkuRepository.search(new ProductSkuSearchCondition(productB.getProductId(), null, null, null, false)))
                .extracting(ProductSku::getSkuCode).containsExactly("SKU-SHOE-270");
        assertThat(productSkuRepository.search(new ProductSkuSearchCondition(null, null, null, null, true)))
                .extracting(ProductSku::getSkuCode).containsExactly("SKU-RED");
    }

    @Test
    @DisplayName("창고·구역: keyword, isActive, 상위 구역, 구역 유형으로 필터링한다")
    void warehousesAndSections() {
        Warehouse seoul = warehouseRepository.save(
                Warehouse.register("WH-SEOUL", "서울 물류센터", "서울", null, BigDecimal.valueOf(1000)));
        Warehouse busan = Warehouse.register("WH-BUSAN", "부산 물류센터", "부산", null, BigDecimal.valueOf(1000));
        busan.deactivate();
        busan = warehouseRepository.save(busan);

        assertThat(warehouseRepository.search(new WarehouseSearchCondition("wh-seoul", null)))
                .extracting(Warehouse::getWarehouseCode).containsExactly("WH-SEOUL");
        assertThat(warehouseRepository.search(new WarehouseSearchCondition(null, false)))
                .extracting(Warehouse::getWarehouseCode).containsExactly("WH-BUSAN");

        WarehouseSection zone = warehouseSectionRepository.save(
                WarehouseSection.register(seoul.getWarehouseId(), null, "A", "A구역", "ZONE", BigDecimal.valueOf(500)));
        warehouseSectionRepository.save(WarehouseSection.register(
                seoul.getWarehouseId(), zone.getSectionId(), "A-01", "A-1랙", "RACK", BigDecimal.valueOf(50)));
        warehouseSectionRepository.save(WarehouseSection.register(
                busan.getWarehouseId(), null, "B", "B구역", "ZONE", BigDecimal.valueOf(500)));

        assertThat(warehouseSectionRepository.search(
                new WarehouseSectionSearchCondition(seoul.getWarehouseId(), null, null, null, null)))
                .extracting(WarehouseSection::getSectionCode).containsExactly("A", "A-01");
        assertThat(warehouseSectionRepository.search(
                new WarehouseSectionSearchCondition(null, zone.getSectionId(), null, null, null)))
                .extracting(WarehouseSection::getSectionCode).containsExactly("A-01");
        assertThat(warehouseSectionRepository.search(
                new WarehouseSectionSearchCondition(null, null, "ZONE", null, null)))
                .extracting(WarehouseSection::getSectionCode).containsExactly("A", "B");
        assertThat(warehouseSectionRepository.search(
                new WarehouseSectionSearchCondition(null, null, null, "1랙", true)))
                .extracting(WarehouseSection::getSectionCode).containsExactly("A-01");
    }
}

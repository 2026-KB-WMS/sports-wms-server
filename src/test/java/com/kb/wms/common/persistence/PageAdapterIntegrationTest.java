package com.kb.wms.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.SortSpec;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.application.port.out.ProductRepository;
import com.kb.wms.product.domain.entity.Brand;
import com.kb.wms.product.domain.entity.Product;

/**
 * 별칭 경로 정렬(조인한 테이블 컬럼 포함), 보조 정렬, 페이지 자르기, 전체 건수가 실제 JPQL에서 맞게 동작하는지 검증한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:page_adapter;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class PageAdapterIntegrationTest {

    private static final SortSpec SPEC = SortSpec.builder()
            .field("productName", "p.name")
            .field("brandName", "b.name")
            .field("id", "p.productId")
            .defaultOrder("productName", SortDirection.ASC)
            .tieBreaker("p.productId")
            .build();

    @Autowired private BrandRepository brandRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private PagingProbeRepository probe;

    @BeforeEach
    void setUp() {
        Long yonex = brandRepository.save(Brand.register("요넥스", null)).getBrandId();
        Long victor = brandRepository.save(Brand.register("빅터", null)).getBrandId();
        // 같은 브랜드·같은 이름이 섞여 있어야 보조 정렬이 필요한 상황이 된다
        productRepository.save(Product.register(yonex, 1L, "P-0001", "라켓 A", null));
        productRepository.save(Product.register(victor, 1L, "P-0002", "라켓 B", null));
        productRepository.save(Product.register(yonex, 1L, "P-0003", "라켓 C", null));
        productRepository.save(Product.register(victor, 1L, "P-0004", "라켓 D", null));
        productRepository.save(Product.register(yonex, 1L, "P-0005", "신발 A", null));
    }

    @Test
    @DisplayName("sort를 생략하면 기본 정렬로, 페이지 크기만큼 자르고 전체 건수를 센다")
    void defaultSortAndPaging() {
        PageCondition condition = PageCondition.of(null, 2, null, SPEC);

        PageResult<PagingProbeRow> result = search(null, condition);

        assertThat(result.items()).extracting(PagingProbeRow::productName).containsExactly("라켓 A", "라켓 B");
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.sort()).isEqualTo("productName,asc");
    }

    @Test
    @DisplayName("조인한 테이블의 컬럼으로 내림차순 정렬하고, 같은 값은 ID로 순서를 고정한다")
    void sortByJoinedColumnWithTieBreaker() {
        PageCondition page1 = PageCondition.of(1, 2, "brandName,desc", SPEC);
        PageCondition page2 = PageCondition.of(2, 2, "brandName,desc", SPEC);
        PageCondition page3 = PageCondition.of(3, 2, "brandName,desc", SPEC);

        // brand desc: 요넥스(3건) → 빅터(2건). 같은 브랜드 안에서는 productId 내림차순
        assertThat(search(null, page1).items()).extracting(PagingProbeRow::productName)
                .containsExactly("신발 A", "라켓 C");
        assertThat(search(null, page2).items()).extracting(PagingProbeRow::productName)
                .containsExactly("라켓 A", "라켓 D");
        assertThat(search(null, page3).items()).extracting(PagingProbeRow::productName)
                .containsExactly("라켓 B");
    }

    @Test
    @DisplayName("검색 조건이 있어도 전체 건수는 조건에 맞는 건수다")
    void totalElementsFollowsFilter() {
        PageResult<PagingProbeRow> result = search("라켓", PageCondition.of(1, 20, "id,asc", SPEC));

        assertThat(result.items()).extracting(PagingProbeRow::productName)
                .containsExactly("라켓 A", "라켓 B", "라켓 C", "라켓 D");
        assertThat(result.totalElements()).isEqualTo(4);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("범위를 넘어선 페이지는 빈 목록이고 전체 건수는 그대로다")
    void pageBeyondEnd() {
        PageResult<PagingProbeRow> result = search(null, PageCondition.of(9, 20, null, SPEC));

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("집계식(count, sum)으로 정렬하고 같은 값은 ID로 고정하며, 전체 건수는 그룹 수다")
    void sortByAggregate() {
        SortSpec spec = SortSpec.builder()
                .field("productCount", "count(p)")
                .field("idSum", "sum(p.productId)")
                .field("brandName", "b.name")
                .defaultOrder("productCount", SortDirection.DESC)
                .tieBreaker("b.brandId")
                .build();

        // 요넥스 3건, 빅터 2건
        PageResult<PagingProbeBrandRow> desc = searchBrands(PageCondition.of(1, 20, null, spec));
        assertThat(desc.items()).extracting(PagingProbeBrandRow::brandName).containsExactly("요넥스", "빅터");
        assertThat(desc.totalElements()).isEqualTo(2);

        PageResult<PagingProbeBrandRow> asc = searchBrands(PageCondition.of(1, 1, "productCount,asc", spec));
        assertThat(asc.items()).extracting(PagingProbeBrandRow::brandName).containsExactly("빅터");
        assertThat(asc.totalPages()).isEqualTo(2);

        PageResult<PagingProbeBrandRow> bySum = searchBrands(PageCondition.of(1, 20, "idSum,desc", spec));
        assertThat(bySum.items()).extracting(PagingProbeBrandRow::idSum).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    private PageResult<PagingProbeBrandRow> searchBrands(PageCondition condition) {
        return PageAdapter.toPageResult(probe.searchBrands(PageAdapter.toPageable(condition)), condition);
    }

    private PageResult<PagingProbeRow> search(String keyword, PageCondition condition) {
        Page<PagingProbeRow> page = probe.search(keyword, PageAdapter.toPageable(condition));
        return PageAdapter.toPageResult(page, condition);
    }
}

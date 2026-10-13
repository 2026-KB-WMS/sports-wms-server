package com.kb.wms.product.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.product.application.port.in.query.BrandSearchCondition;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.domain.entity.Brand;

/**
 * 브랜드 코드 저장: 코드가 없는 기존 브랜드는 여러 개 공존하고, 같은 코드는 유니크 제약으로 막으며, 목록 검색은 코드도 본다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class BrandCodePersistenceTest {

    @Autowired
    BrandRepository brandRepository;

    @Test
    @DisplayName("코드가 없는 브랜드는 여러 개 저장할 수 있다")
    void multipleBrandsWithoutCode() {
        Brand a = brandRepository.save(Brand.register("코드없음A", null));
        Brand b = brandRepository.save(Brand.register("코드없음B", null));

        assertThat(a.getBrandCode()).isNull();
        assertThat(b.getBrandCode()).isNull();
    }

    @Test
    @DisplayName("저장한 코드는 그대로 읽히고 existsByBrandCode로 확인된다")
    void codeIsStoredAndFound() {
        Brand saved = brandRepository.save(Brand.register("NKE", "나이키", null));

        assertThat(brandRepository.findById(saved.getBrandId()).orElseThrow().getBrandCode()).isEqualTo("NKE");
        assertThat(brandRepository.existsByBrandCode("NKE")).isTrue();
        assertThat(brandRepository.existsByBrandCodeAndBrandIdNot("NKE", saved.getBrandId())).isFalse();
        assertThat(brandRepository.existsByBrandCodeAndBrandIdNot("NKE", saved.getBrandId() + 1)).isTrue();
    }

    @Test
    @DisplayName("같은 코드는 DB 유니크 제약으로 저장할 수 없다")
    void duplicateCodeRejected() {
        brandRepository.save(Brand.register("DUP", "브랜드1", null));

        assertThatThrownBy(() -> {
            brandRepository.save(Brand.register("DUP", "브랜드2", null));
            brandRepository.findById(-1L); // flush 유도
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("목록 keyword는 브랜드 코드로도 검색한다(대소문자 무시)")
    void searchByCode() {
        brandRepository.save(Brand.register("QZX", "검색용브랜드", null));

        List<Brand> found = brandRepository.search(new BrandSearchCondition("qzx", null));

        assertThat(found).extracting(Brand::getName).contains("검색용브랜드");
    }
}

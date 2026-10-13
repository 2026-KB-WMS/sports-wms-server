package com.kb.wms.product.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.product.application.port.out.CategoryOptionGroupRepository;
import com.kb.wms.product.application.port.out.ProductSpecRepository;
import com.kb.wms.product.domain.entity.CategoryOptionGroup;
import com.kb.wms.product.domain.entity.ProductSpec;
import com.kb.wms.product.domain.enums.OptionType;

/**
 * 카테고리 옵션 그룹 지정·상품 스펙의 "전체 교체"가 같은 행을 다시 넣어도 유니크 제약에 걸리지 않는지 확인한다.
 * 엔티티에는 FK가 없으므로(ID 참조) 상위 행 없이 ID만으로 저장한다. category·option_value 테이블은
 * H2 DDL에서 만들어지지 않아(TINYINT, 예약어 value) 이 테스트에서 다루지 않는다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class OptionMappingPersistenceTest {

    @Autowired
    CategoryOptionGroupRepository categoryOptionGroupRepository;
    @Autowired
    ProductSpecRepository productSpecRepository;

    @Test
    @DisplayName("같은 그룹을 다시 지정해 교체해도 유니크 제약에 걸리지 않고, 빠진 그룹은 지워진다")
    void replaceAllMappings() {
        categoryOptionGroupRepository.replaceAll(2001L, List.of(
                CategoryOptionGroup.assign(2001L, 11L, OptionType.SKU, 0),
                CategoryOptionGroup.assign(2001L, 12L, OptionType.SKU, 1)));
        categoryOptionGroupRepository.replaceAll(2001L, List.of(
                CategoryOptionGroup.assign(2001L, 11L, OptionType.SPEC, 0)));

        List<CategoryOptionGroup> saved = categoryOptionGroupRepository.findByCategoryIds(List.of(2001L));
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getOptionGroupId()).isEqualTo(11L);
        assertThat(saved.get(0).getOptionType()).isEqualTo(OptionType.SPEC);
    }

    @Test
    @DisplayName("빈 목록으로 교체하면 그 카테고리의 지정만 모두 지워진다")
    void replaceAllWithEmptyClears() {
        categoryOptionGroupRepository.replaceAll(2002L, List.of(
                CategoryOptionGroup.assign(2002L, 11L, OptionType.SKU, 0)));
        categoryOptionGroupRepository.replaceAll(2003L, List.of(
                CategoryOptionGroup.assign(2003L, 11L, OptionType.SKU, 0)));

        categoryOptionGroupRepository.replaceAll(2002L, List.of());

        assertThat(categoryOptionGroupRepository.findByCategoryIds(List.of(2002L))).isEmpty();
        assertThat(categoryOptionGroupRepository.findByCategoryIds(List.of(2003L))).hasSize(1);
    }

    @Test
    @DisplayName("여러 카테고리의 지정을 한 번에 조회한다")
    void findByCategoryIds() {
        categoryOptionGroupRepository.replaceAll(2004L, List.of(
                CategoryOptionGroup.assign(2004L, 11L, OptionType.SKU, 0)));
        categoryOptionGroupRepository.replaceAll(2005L, List.of(
                CategoryOptionGroup.assign(2005L, 12L, OptionType.SPEC, 0)));

        assertThat(categoryOptionGroupRepository.findByCategoryIds(List.of(2004L, 2005L)))
                .extracting(CategoryOptionGroup::getCategoryId).containsExactlyInAnyOrder(2004L, 2005L);
        assertThat(categoryOptionGroupRepository.findByCategoryIds(List.of())).isEmpty();
    }

    @Test
    @DisplayName("상품 스펙을 같은 값으로 다시 교체해도 유니크 제약에 걸리지 않는다")
    void replaceAllSpecs() {
        productSpecRepository.replaceAll(1001L, List.of(31L));
        productSpecRepository.replaceAll(1001L, List.of(31L, 32L));
        productSpecRepository.replaceAll(1001L, List.of(32L));

        List<ProductSpec> specs = productSpecRepository.findByProductId(1001L);
        assertThat(specs).extracting(ProductSpec::getOptionValueId).containsExactly(32L);
        assertThat(productSpecRepository.existsByProductId(1001L)).isTrue();
        assertThat(productSpecRepository.existsByProductId(1002L)).isFalse();
    }
}

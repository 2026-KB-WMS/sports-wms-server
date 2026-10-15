package com.kb.wms.inbound.adapter.out.persistence;

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

import com.kb.wms.inbound.application.port.out.SkuSupplierRepository;
import com.kb.wms.inbound.domain.entity.SkuSupplier;

import jakarta.persistence.EntityManager;

/**
 * SKU↔공급처 매핑 영속성 어댑터 검증. 테스트 스키마는 JPA 엔티티로 만들어 FK 제약이 없으므로 임의의 ID를 쓴다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class SkuSupplierPersistenceAdapterTest {

    @Autowired SkuSupplierRepository skuSupplierRepository;
    @Autowired EntityManager entityManager;

    @Test
    @DisplayName("매핑을 저장하면 ID와 생성 시각이 채워지고 SKU 기준으로 조회된다")
    void saveAndFindBySkuId() {
        SkuSupplier saved = skuSupplierRepository.save(SkuSupplier.register(10L, 3L));
        skuSupplierRepository.save(SkuSupplier.register(10L, 4L));
        skuSupplierRepository.save(SkuSupplier.register(11L, 3L));

        assertThat(saved.getSkuSupplierId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(skuSupplierRepository.findBySkuId(10L))
                .extracting(SkuSupplier::getSupplierId).containsExactly(3L, 4L);
        assertThat(skuSupplierRepository.findBySkuId(99L)).isEmpty();
    }

    @Test
    @DisplayName("여러 SKU의 매핑을 한 번에 조회하고, 빈 목록이면 빈 결과다")
    void findBySkuIdIn() {
        skuSupplierRepository.save(SkuSupplier.register(10L, 3L));
        skuSupplierRepository.save(SkuSupplier.register(11L, 4L));
        skuSupplierRepository.save(SkuSupplier.register(12L, 5L));

        assertThat(skuSupplierRepository.findBySkuIdIn(List.of(10L, 11L)))
                .extracting(SkuSupplier::getSkuId).containsExactlyInAnyOrder(10L, 11L);
        assertThat(skuSupplierRepository.findBySkuIdIn(List.of())).isEmpty();
    }

    @Test
    @DisplayName("(SKU, 공급처) 쌍의 존재 여부를 확인한다")
    void existsBySkuIdAndSupplierId() {
        skuSupplierRepository.save(SkuSupplier.register(10L, 3L));

        assertThat(skuSupplierRepository.existsBySkuIdAndSupplierId(10L, 3L)).isTrue();
        assertThat(skuSupplierRepository.existsBySkuIdAndSupplierId(10L, 4L)).isFalse();
        assertThat(skuSupplierRepository.existsBySkuIdAndSupplierId(11L, 3L)).isFalse();
    }

    @Test
    @DisplayName("같은 (SKU, 공급처) 쌍은 두 번 저장할 수 없다")
    void duplicatePair() {
        skuSupplierRepository.save(SkuSupplier.register(10L, 3L));
        entityManager.flush();

        assertThatThrownBy(() -> {
            skuSupplierRepository.save(SkuSupplier.register(10L, 3L));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("매핑을 삭제하면 해당 쌍만 사라지고, 없는 쌍은 0건이다")
    void deleteBySkuIdAndSupplierId() {
        skuSupplierRepository.save(SkuSupplier.register(10L, 3L));
        skuSupplierRepository.save(SkuSupplier.register(10L, 4L));

        assertThat(skuSupplierRepository.deleteBySkuIdAndSupplierId(10L, 3L)).isEqualTo(1L);
        assertThat(skuSupplierRepository.deleteBySkuIdAndSupplierId(10L, 3L)).isZero();

        entityManager.clear();
        assertThat(skuSupplierRepository.findBySkuId(10L))
                .extracting(SkuSupplier::getSupplierId).containsExactly(4L);
    }
}

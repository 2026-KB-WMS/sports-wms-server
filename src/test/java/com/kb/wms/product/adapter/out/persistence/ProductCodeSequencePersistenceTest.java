package com.kb.wms.product.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.product.application.port.out.ProductCodeSequenceRepository;

/**
 * 브랜드+카테고리 조합별로 번호가 1부터 올라가고 서로 영향을 주지 않는지 확인한다.
 * 엔티티에는 FK가 없으므로(ID 참조) 브랜드·카테고리 행 없이 ID만으로 채번한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class ProductCodeSequencePersistenceTest {

    @Autowired
    ProductCodeSequenceRepository sequenceRepository;

    @Test
    @DisplayName("같은 조합은 1부터 하나씩 올라가고, 다른 조합은 따로 센다")
    void nextPerCombination() {
        assertThat(sequenceRepository.next(3001L, 4001L)).isEqualTo(1L);
        assertThat(sequenceRepository.next(3001L, 4001L)).isEqualTo(2L);
        assertThat(sequenceRepository.next(3001L, 4002L)).isEqualTo(1L);
        assertThat(sequenceRepository.next(3002L, 4001L)).isEqualTo(1L);
        assertThat(sequenceRepository.next(3001L, 4001L)).isEqualTo(3L);
    }
}

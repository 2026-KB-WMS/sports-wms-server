package com.kb.wms.product.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.kb.wms.product.adapter.out.persistence.repository.SkuPriceHistoryJpaRepository;
import com.kb.wms.product.application.port.out.SkuPriceHistoryRepository;
import com.kb.wms.product.domain.entity.SkuPriceHistory;

/** SKU 단가 변경 이력이 이전·이후 단가와 처리자를 그대로 저장하는지 확인한다. */
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class SkuPriceHistoryPersistenceAdapterTest {

    @Autowired
    private SkuPriceHistoryRepository skuPriceHistoryRepository;

    @Autowired
    private SkuPriceHistoryJpaRepository skuPriceHistoryJpaRepository;

    @AfterEach
    void cleanUp() {
        skuPriceHistoryJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("이전·이후 매입·공급 단가와 처리자, 변경 시각을 저장한다")
    void save() {
        SkuPriceHistory saved = skuPriceHistoryRepository.save(SkuPriceHistory.record(
                7L, BigDecimal.valueOf(10000), BigDecimal.valueOf(12000),
                BigDecimal.valueOf(15000), BigDecimal.valueOf(15000), 3L));

        assertThat(saved.getSkuPriceHistoryId()).isNotNull();
        SkuPriceHistory found = skuPriceHistoryJpaRepository.findById(saved.getSkuPriceHistoryId())
                .orElseThrow().toDomain();
        assertThat(found.getSkuId()).isEqualTo(7L);
        assertThat(found.getPreviousPurchasePrice()).isEqualByComparingTo("10000");
        assertThat(found.getNewPurchasePrice()).isEqualByComparingTo("12000");
        assertThat(found.getPreviousSupplyPrice()).isEqualByComparingTo("15000");
        assertThat(found.getNewSupplyPrice()).isEqualByComparingTo("15000");
        assertThat(found.getChangedBy()).isEqualTo(3L);
        assertThat(found.getChangedAt()).isNotNull();
    }
}

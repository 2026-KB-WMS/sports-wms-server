package com.kb.wms.warehouse.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;

/**
 * 같은 상위 아래 활성 구역 수용량 합 쿼리: 최상위·하위 구분, 비활성·다른 창고·제외 대상 처리를 확인한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class WarehouseSectionPersistenceAdapterTest {

    @Autowired WarehouseRepository warehouseRepository;
    @Autowired WarehouseSectionRepository warehouseSectionRepository;

    private Long warehouse(String code) {
        return warehouseRepository.save(
                Warehouse.register(code, code, "서울시", null, BigDecimal.valueOf(1000))).getWarehouseId();
    }

    private WarehouseSection section(Long warehouseId, Long parentId, String code, int capacity) {
        return warehouseSectionRepository.save(WarehouseSection.register(
                warehouseId, parentId, code, code, "ZONE", BigDecimal.valueOf(capacity)));
    }

    @Test
    @DisplayName("상위가 null이면 창고의 최상위 활성 구역만 합산하고 하위 구역은 제외한다")
    void sumTopLevel() {
        Long wh = warehouse("WH-SUM-1");
        WarehouseSection a = section(wh, null, "A", 100);
        section(wh, null, "B", 200);
        section(wh, a.getSectionId(), "A-1", 50);

        assertThat(warehouseSectionRepository.sumActiveCapacity(wh, null, null)).isEqualByComparingTo("300");
    }

    @Test
    @DisplayName("상위 구역을 지정하면 그 직속 하위 활성 구역만 합산한다")
    void sumChildren() {
        Long wh = warehouse("WH-SUM-2");
        WarehouseSection a = section(wh, null, "A", 100);
        WarehouseSection b = section(wh, null, "B", 200);
        section(wh, a.getSectionId(), "A-1", 30);
        section(wh, a.getSectionId(), "A-2", 20);
        section(wh, b.getSectionId(), "B-1", 70);

        assertThat(warehouseSectionRepository.sumActiveCapacity(wh, a.getSectionId(), null)).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("비활성 구역과 제외 대상, 다른 창고의 구역은 합산하지 않는다")
    void sumExcludesInactiveExcludedAndOtherWarehouse() {
        Long wh = warehouse("WH-SUM-3");
        Long other = warehouse("WH-SUM-4");
        WarehouseSection a = section(wh, null, "A", 100);
        WarehouseSection inactive = section(wh, null, "B", 200);
        inactive.deactivate();
        warehouseSectionRepository.save(inactive);
        section(other, null, "C", 400);

        assertThat(warehouseSectionRepository.sumActiveCapacity(wh, null, null)).isEqualByComparingTo("100");
        assertThat(warehouseSectionRepository.sumActiveCapacity(wh, null, a.getSectionId())).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("합산 대상이 없으면 0을 반환한다")
    void sumEmpty() {
        Long wh = warehouse("WH-SUM-5");

        assertThat(warehouseSectionRepository.sumActiveCapacity(wh, null, null)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("하위 구역 존재 여부는 비활성 하위 구역도 포함하고, 삭제하면 사라진다")
    void existsChild_includesInactiveAndDelete() {
        Long wh = warehouse("WH-DEL-1");
        WarehouseSection parent = section(wh, null, "A", 100);
        WarehouseSection child = section(wh, parent.getSectionId(), "A-1", 10);
        child.deactivate();
        warehouseSectionRepository.save(child);

        assertThat(warehouseSectionRepository.existsActiveChild(parent.getSectionId())).isFalse();
        assertThat(warehouseSectionRepository.existsChild(parent.getSectionId())).isTrue();

        warehouseSectionRepository.deleteById(child.getSectionId());

        assertThat(warehouseSectionRepository.existsChild(parent.getSectionId())).isFalse();
        assertThat(warehouseSectionRepository.findById(child.getSectionId())).isEmpty();
    }
}

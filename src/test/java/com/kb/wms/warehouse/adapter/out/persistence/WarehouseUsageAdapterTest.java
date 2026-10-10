package com.kb.wms.warehouse.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.inbound.adapter.out.persistence.entity.InboundLineJpaEntity;
import com.kb.wms.inbound.adapter.out.persistence.repository.InboundLineJpaRepository;
import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.adapter.out.persistence.repository.InventoryLotJpaRepository;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.warehouse.application.port.out.WarehouseUsagePort;

/**
 * 구역 삭제 전 참조 확인: 재고 로트 행(수량 0 포함)과 입고 검수 항목의 합격·불량 구역 지정을 센다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class WarehouseUsageAdapterTest {

    @Autowired WarehouseUsagePort warehouseUsagePort;
    @Autowired InventoryLotJpaRepository inventoryLotJpaRepository;
    @Autowired InboundLineJpaRepository inboundLineJpaRepository;

    private InboundLineJpaEntity line(Long acceptedSectionId, Long defectSectionId) {
        return InboundLineJpaEntity.builder()
                .inboundId(1L).purchaseOrderLineId(1L).lotId(1L)
                .acceptedSectionId(acceptedSectionId).defectSectionId(defectSectionId)
                .receivedQuantity(10L).acceptedQuantity(10L).defectiveQuantity(0L)
                .receivedUnitPrice(BigDecimal.TEN).lineAmount(BigDecimal.valueOf(100))
                .receivedAt(LocalDateTime.of(2026, 10, 10, 9, 0)).receivedBy(1L)
                .build();
    }

    @Test
    @DisplayName("참조하는 로트 행이나 검수 항목이 없으면 false")
    void notReferenced() {
        assertThat(warehouseUsagePort.isSectionReferenced(9001L)).isFalse();
    }

    @Test
    @DisplayName("수량이 0이어도 재고 로트 행이 있으면 참조 중이다")
    void referencedByInventoryLotRow() {
        inventoryLotJpaRepository.save(InventoryLotJpaEntity.builder()
                .sectionId(9002L).lotId(1L).onHandQuantity(0L).allocatedQuantity(0L)
                .qualityStatus(QualityStatus.AVAILABLE).build());

        assertThat(warehouseUsagePort.isSectionReferenced(9002L)).isTrue();
    }

    @Test
    @DisplayName("입고 검수 항목의 합격 구역이나 불량 구역으로 지정되면 참조 중이다")
    void referencedByInboundLine() {
        inboundLineJpaRepository.save(line(9003L, 9004L));

        assertThat(warehouseUsagePort.isSectionReferenced(9003L)).isTrue();
        assertThat(warehouseUsagePort.isSectionReferenced(9004L)).isTrue();
        assertThat(warehouseUsagePort.isSectionReferenced(9005L)).isFalse();
    }
}

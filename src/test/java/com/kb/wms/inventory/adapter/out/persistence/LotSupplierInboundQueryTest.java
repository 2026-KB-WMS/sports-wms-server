package com.kb.wms.inventory.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.InboundLine;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.LotUseCase;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryDetail;
import com.kb.wms.inventory.application.port.in.result.LotInboundView;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.InventoryQueryRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.inventory.exception.InventoryErrorCode;
import com.kb.wms.product.adapter.out.persistence.entity.ProductSkuJpaEntity;
import com.kb.wms.product.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.kb.wms.product.domain.enums.ProductStatus;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseJpaRepository;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseSectionJpaRepository;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

/**
 * 로트·재고 조회의 공급처명(supplierName), 로트 입고 이력(inbounds), 공급처 필터 404 검증(조회 전용 조인, ADR-007).
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class LotSupplierInboundQueryTest {

    @Autowired private InventoryQueryRepository inventoryQueryRepository;
    @Autowired private InventoryQueryUseCase inventoryQueryUseCase;
    @Autowired private LotUseCase lotUseCase;
    @Autowired private LotRepository lotRepository;
    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Autowired private InventoryLotRepository inventoryLotRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private InboundRepository inboundRepository;
    @Autowired private ProductSkuJpaRepository productSkuJpaRepository;
    @Autowired private WarehouseJpaRepository warehouseJpaRepository;
    @Autowired private WarehouseSectionJpaRepository warehouseSectionJpaRepository;

    private Long supplierId;
    private Long warehouseId;
    private Long sectionId;
    private Long lotId;
    private Long inventoryLotId;
    private Long orphanSupplierLotId;

    @BeforeEach
    void setUp() {
        Long skuId = productSkuJpaRepository.save(ProductSkuJpaEntity.builder()
                .productId(1L).skuCode("SKU-L").name("상품L").unit("EA").currentPurchasePrice(java.math.BigDecimal.ZERO).currentSupplyPrice(java.math.BigDecimal.ZERO)
                .safetyStockQuantity(0L).status(ProductStatus.ACTIVE)
                .build()).getSkuId();
        warehouseId = warehouseJpaRepository.save(WarehouseJpaEntity.builder()
                .warehouseCode("WH-L").name("서울센터").address("주소").totalCapacity(BigDecimal.valueOf(1000))
                .status(WarehouseStatus.ACTIVE)
                .build()).getWarehouseId();
        sectionId = warehouseSectionJpaRepository.save(WarehouseSectionJpaEntity.builder()
                .warehouseId(warehouseId).sectionCode("A-01").name("1구역").sectionType("RACK")
                .capacity(BigDecimal.valueOf(500)).currentCapacity(BigDecimal.ZERO)
                .status(WarehouseStatus.ACTIVE)
                .build()).getSectionId();
        supplierId = supplierRepository.save(Supplier.register(
                "SUP-L", "한빛식품", "김담당", "02-111-2222", "a@b.com", "서울")).getSupplierId();

        lotId = lotRepository.save(Lot.register(skuId, supplierId, "LOT-L1", null,
                LocalDate.of(2027, 1, 1), BigDecimal.valueOf(1000))).getLotId();
        // 존재하지 않는 공급처를 가리키는 로트도 left join으로 조회돼야 한다(supplierName null).
        orphanSupplierLotId = lotRepository.save(Lot.register(skuId, 9_999L, "LOT-L2", null,
                LocalDate.of(2027, 2, 1), BigDecimal.valueOf(1000))).getLotId();

        InventoryLot inventoryLot = InventoryLot.open(sectionId, lotId, QualityStatus.AVAILABLE);
        inventoryLot.increase(10);
        inventoryLotId = inventoryLotRepository.save(inventoryLot).getInventoryLotId();
    }

    @Test
    @DisplayName("로트 목록·단건 조회에 공급처명이 담기고, 공급처 행이 없으면 supplierName은 null이다")
    void lotSummary_hasSupplierName() {
        LotSummary lot = inventoryQueryRepository.findLot(lotId).orElseThrow();
        assertThat(lot.supplierId()).isEqualTo(supplierId);
        assertThat(lot.supplierName()).isEqualTo("한빛식품");

        assertThat(inventoryQueryRepository.findLots(LotSearchCondition.unscoped(null, supplierId, null, null)))
                .extracting(LotSummary::lotNumber)
                .containsExactly("LOT-L1");

        assertThat(inventoryQueryRepository.findLot(orphanSupplierLotId).orElseThrow().supplierName()).isNull();
    }

    @Test
    @DisplayName("로트 범위: 담당 창고에 재고가 있는 로트만 보이고, 재고도 입고 완료 이력도 없는 창고는 비어 있다")
    void lots_warehouseScope() {
        assertThat(inventoryQueryRepository.findLots(new LotSearchCondition(null, null, null, null, List.of(warehouseId))))
                .extracting(LotSummary::lotNumber).containsExactly("LOT-L1");
        assertThat(inventoryQueryRepository.findLots(new LotSearchCondition(null, null, null, null, List.of(9_999L))))
                .isEmpty();
        assertThat(inventoryQueryRepository.findLots(new LotSearchCondition(null, null, null, null, List.of())))
                .isEmpty();
        assertThat(inventoryQueryRepository.findLots(new LotSearchCondition(null, null, null, null, null)))
                .hasSize(2);
    }

    @Test
    @DisplayName("재고 상세에 공급처명이 담긴다")
    void inventoryDetail_hasSupplierName() {
        InventoryDetail detail = inventoryQueryUseCase.getInventory(inventoryLotId, HQ);

        assertThat(detail.supplierId()).isEqualTo(supplierId);
        assertThat(detail.supplierName()).isEqualTo("한빛식품");
    }

    @Test
    @DisplayName("공급처 존재 확인: 있는 공급처만 true")
    void existsSupplier() {
        assertThat(inventoryQueryRepository.existsSupplier(supplierId)).isTrue();
        assertThat(inventoryQueryRepository.existsSupplier(9_999L)).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 supplierId로 로트를 필터링하면 404 SUPPLIER_NOT_FOUND이고, 있는 공급처는 목록을 반환한다")
    void getLots_supplierFilter() {
        assertThatThrownBy(() -> lotUseCase.getLots(LotSearchCondition.unscoped(null, 9_999L, null, null), HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.SUPPLIER_NOT_FOUND.name());

        assertThat(lotUseCase.getLots(LotSearchCondition.unscoped(null, supplierId, null, null), HQ)).hasSize(1);
    }

    @Test
    @DisplayName("로트 입고 이력은 이 로트의 입고 완료 항목만, 최근 입고 순으로 반환한다")
    void lotInbounds_onlyCompletedLinesOfThisLot() {
        Long purchaseOrderId = 1L;
        Long completed1 = inbound("IB-L-1", purchaseOrderId, InboundStatus.COMPLETED, LocalDateTime.of(2026, 10, 1, 10, 0));
        Long completed2 = inbound("IB-L-2", purchaseOrderId, InboundStatus.COMPLETED, LocalDateTime.of(2026, 10, 3, 10, 0));
        Long inspecting = inbound("IB-L-3", purchaseOrderId, InboundStatus.INSPECTING, null);
        Long canceled = inbound("IB-L-4", purchaseOrderId, InboundStatus.CANCELED, null);
        Long otherLotInbound = inbound("IB-L-5", purchaseOrderId, InboundStatus.COMPLETED, LocalDateTime.of(2026, 10, 2, 10, 0));
        line(completed1, lotId, 100, 95, 5, "1200.00");
        line(completed2, lotId, 50, 50, 0, "1300.00");
        line(inspecting, lotId, 10, 10, 0, "1200.00");
        line(canceled, lotId, 10, 10, 0, "1200.00");
        line(otherLotInbound, orphanSupplierLotId, 10, 10, 0, "1200.00");

        List<LotInboundView> inbounds = lotUseCase.getLotInbounds(lotId, HQ);

        assertThat(inbounds).extracting(LotInboundView::inboundNo).containsExactly("IB-L-2", "IB-L-1");
        LotInboundView first = inbounds.get(1);
        assertThat(first.inboundId()).isEqualTo(completed1);
        assertThat(first.warehouseId()).isEqualTo(warehouseId);
        assertThat(first.receivedAt()).isEqualTo(LocalDateTime.of(2026, 10, 1, 10, 0));
        assertThat(first.receivedQuantity()).isEqualTo(100L);
        assertThat(first.acceptedQuantity()).isEqualTo(95L);
        assertThat(first.defectiveQuantity()).isEqualTo(5L);
        assertThat(first.receivedUnitPrice()).isEqualByComparingTo("1200.00");
    }

    @Test
    @DisplayName("입고 이력이 없는 로트는 빈 목록, 없는 로트는 404 LOT_NOT_FOUND")
    void lotInbounds_emptyAndNotFound() {
        assertThat(lotUseCase.getLotInbounds(lotId, HQ)).isEmpty();

        assertThatThrownBy(() -> lotUseCase.getLotInbounds(9_999L, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.LOT_NOT_FOUND.name());
        assertThatThrownBy(() -> lotUseCase.getLot(9_999L, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.LOT_NOT_FOUND.name());
    }

    @Test
    @DisplayName("없는 재고·필터 대상은 도메인 전용 404 코드를 반환한다")
    void notFoundCodes() {
        assertThatThrownBy(() -> inventoryQueryUseCase.getInventory(9_999L, HQ))
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.INVENTORY_NOT_FOUND.name());
    }

    private Long inbound(String no, Long purchaseOrderId, InboundStatus status, LocalDateTime receivedAt) {
        return inboundRepository.save(Inbound.builder()
                .inboundNo(no).purchaseOrderId(purchaseOrderId).warehouseId(warehouseId).status(status)
                .arrivedAt(LocalDateTime.of(2026, 9, 30, 9, 0)).receivedAt(receivedAt)
                .build()).getInboundId();
    }

    private void line(Long inboundId, Long lotId, long received, long accepted, long defective, String price) {
        inboundRepository.saveLines(List.of(InboundLine.register(
                inboundId, 1L, lotId, sectionId, sectionId, received, accepted, defective,
                new BigDecimal(price), null, null, LocalDateTime.of(2026, 10, 1, 9, 0), 1L)));
    }
}

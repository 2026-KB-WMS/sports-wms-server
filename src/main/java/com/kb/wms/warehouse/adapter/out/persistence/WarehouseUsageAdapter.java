package com.kb.wms.warehouse.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.application.port.in.InboundUsageUseCase;
import com.kb.wms.outbound.application.port.in.OutboundUsageUseCase;
import com.kb.wms.storeorder.application.port.in.StoreOrderUsageUseCase;
import com.kb.wms.warehouse.application.port.out.WarehouseUsagePort;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

/**
 * 창고 비활성화·구역 삭제 전 사용 여부 확인(읽기 전용). 진행 중 입고·창고 발주·출고·지점 발주는 각 업무 도메인의
 * 유스케이스가 판단해 결과만 돌려주고, 구역 참조 여부만 ADR-007에 따라 JPQL로 직접 조회한다.
 */
@Component
@RequiredArgsConstructor
public class WarehouseUsageAdapter implements WarehouseUsagePort {

    private final InboundUsageUseCase inboundUsageUseCase;
    private final OutboundUsageUseCase outboundUsageUseCase;
    private final StoreOrderUsageUseCase storeOrderUsageUseCase;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean hasInProgressInbounds(Long warehouseId) {
        return inboundUsageUseCase.hasInProgressInboundForWarehouse(warehouseId);
    }

    @Override
    public boolean hasInProgressPurchaseOrders(Long warehouseId) {
        return inboundUsageUseCase.hasInProgressPurchaseOrderForWarehouse(warehouseId);
    }

    @Override
    public boolean hasInProgressOutbounds(Long warehouseId) {
        return outboundUsageUseCase.hasInProgressOutboundForWarehouse(warehouseId);
    }

    @Override
    public boolean hasInProgressStoreOrders(Long warehouseId) {
        return storeOrderUsageUseCase.hasInProgressStoreOrderForWarehouse(warehouseId);
    }

    @Override
    public boolean isSectionReferenced(Long sectionId) {
        Long lotRows = entityManager.createQuery(
                        "select count(il) from InventoryLotJpaEntity il where il.sectionId = :sectionId", Long.class)
                .setParameter("sectionId", sectionId)
                .getSingleResult();
        if (lotRows != null && lotRows > 0) {
            return true;
        }
        Long inboundLines = entityManager.createQuery("""
                        select count(l) from InboundLineJpaEntity l
                        where l.acceptedSectionId = :sectionId or l.defectSectionId = :sectionId
                        """, Long.class)
                .setParameter("sectionId", sectionId)
                .getSingleResult();
        return inboundLines != null && inboundLines > 0;
    }
}

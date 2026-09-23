package com.kb.wms.warehouse.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseSectionJpaRepository;
import com.kb.wms.warehouse.application.port.out.WarehouseSectionRepository;
import com.kb.wms.warehouse.domain.entity.WarehouseSection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WarehouseSectionPersistenceAdapter implements WarehouseSectionRepository {

    private final WarehouseSectionJpaRepository warehouseSectionJpaRepository;

    @Override
    public WarehouseSection save(WarehouseSection section) {
        WarehouseSectionJpaEntity saved =
                warehouseSectionJpaRepository.save(WarehouseSectionJpaEntity.fromDomain(section));
        return saved.toDomain();
    }

    @Override
    public Optional<WarehouseSection> findById(Long sectionId) {
        return warehouseSectionJpaRepository.findById(sectionId).map(WarehouseSectionJpaEntity::toDomain);
    }

    @Override
    public List<WarehouseSection> findAll(Long warehouseId) {
        return warehouseSectionJpaRepository.findAllByFilter(warehouseId).stream()
                .map(WarehouseSectionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByWarehouseIdAndSectionCode(Long warehouseId, String sectionCode) {
        return warehouseSectionJpaRepository.existsByWarehouseIdAndSectionCode(warehouseId, sectionCode);
    }
}

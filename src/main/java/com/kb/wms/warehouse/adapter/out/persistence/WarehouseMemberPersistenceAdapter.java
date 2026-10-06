package com.kb.wms.warehouse.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.common.persistence.SearchKeyword;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseMemberJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseMemberJpaRepository;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMemberView;
import com.kb.wms.warehouse.application.port.out.WarehouseMemberRepository;
import com.kb.wms.warehouse.domain.entity.WarehouseMember;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WarehouseMemberPersistenceAdapter implements WarehouseMemberRepository {

    private final WarehouseMemberJpaRepository warehouseMemberJpaRepository;

    @Override
    public WarehouseMember save(WarehouseMember member) {
        WarehouseMemberJpaEntity saved =
                warehouseMemberJpaRepository.save(WarehouseMemberJpaEntity.fromDomain(member));
        return saved.toDomain();
    }

    @Override
    public Optional<WarehouseMember> findById(Long warehouseMemberId) {
        return warehouseMemberJpaRepository.findById(warehouseMemberId).map(WarehouseMemberJpaEntity::toDomain);
    }

    @Override
    public List<WarehouseMemberView> search(Long warehouseId, Long userId, String keyword) {
        return warehouseMemberJpaRepository.search(warehouseId, userId, SearchKeyword.normalize(keyword));
    }

    @Override
    public List<WarehouseMember> findByUserId(Long userId) {
        return warehouseMemberJpaRepository.findByUserId(userId).stream()
                .map(WarehouseMemberJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByWarehouseIdAndUserId(Long warehouseId, Long userId) {
        return warehouseMemberJpaRepository.existsByWarehouseIdAndUserId(warehouseId, userId);
    }

    @Override
    public void deleteById(Long warehouseMemberId) {
        warehouseMemberJpaRepository.deleteById(warehouseMemberId);
    }
}

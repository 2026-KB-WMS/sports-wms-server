package com.kb.wms.warehouse.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.warehouse.domain.entity.WarehouseMember;

/**
 * 창고 관리자 배정 영속성 아웃바운드 포트.
 */
public interface WarehouseMemberRepository {

    WarehouseMember save(WarehouseMember member);

    Optional<WarehouseMember> findById(Long warehouseMemberId);

    /**
     * warehouseId가 null이면 조건을 무시하고 조회한다.
     */
    List<WarehouseMember> findAll(Long warehouseId);

    List<WarehouseMember> findByUserId(Long userId);

    boolean existsByWarehouseIdAndUserId(Long warehouseId, Long userId);

    void deleteById(Long warehouseMemberId);
}

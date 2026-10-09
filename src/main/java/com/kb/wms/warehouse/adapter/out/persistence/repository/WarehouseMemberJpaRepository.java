package com.kb.wms.warehouse.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseMemberJpaEntity;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMemberView;

public interface WarehouseMemberJpaRepository extends JpaRepository<WarehouseMemberJpaEntity, Long> {

    boolean existsByWarehouseIdAndUserId(Long warehouseId, Long userId);

    List<WarehouseMemberJpaEntity> findByUserId(Long userId);

    /** 사용자 테이블을 ID로 조인해 이름·로그인 아이디를 붙이고 keyword로 거른다(읽기 전용 조인). */
    @Query("""
            select new com.kb.wms.warehouse.application.port.in.result.WarehouseMemberView(
                m.warehouseMemberId, m.warehouseId, m.userId, u.name, u.loginId, m.memberRole, m.assignedAt)
            from WarehouseMemberJpaEntity m
            join UserJpaEntity u on u.userId = m.userId
            where (:warehouseId is null or m.warehouseId = :warehouseId)
              and (:userId is null or m.userId = :userId)
              and (:keyword is null
                   or lower(u.name) like lower(concat('%', :keyword, '%'))
                   or lower(u.loginId) like lower(concat('%', :keyword, '%')))
            """)
    List<WarehouseMemberView> search(@Param("warehouseId") Long warehouseId,
                                     @Param("userId") Long userId,
                                     @Param("keyword") String keyword);
}

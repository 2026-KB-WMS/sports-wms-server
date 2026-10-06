package com.kb.wms.auth.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.kb.wms.auth.adapter.out.persistence.entity.UserJpaEntity;

/** 회원 조회 전용. 다른 도메인 테이블(창고·지점 소속)은 ID 기준 읽기 전용으로만 읽는다(ADR-007). */
public interface UserQueryJpaRepository extends Repository<UserJpaEntity, Long> {

    @Query("""
            select m.warehouseId
            from WarehouseMemberJpaEntity m
            where m.userId = :userId
            order by m.warehouseId asc
            """)
    List<Long> findWarehouseIds(@Param("userId") Long userId);

    @Query("""
            select m.storeId
            from StoreMemberJpaEntity m
            where m.userId = :userId
            order by m.storeId asc
            """)
    List<Long> findStoreIds(@Param("userId") Long userId);

    @Query("select u from UserJpaEntity u where u.userId in :userIds")
    List<UserJpaEntity> findAllByIds(@Param("userIds") Collection<Long> userIds);
}

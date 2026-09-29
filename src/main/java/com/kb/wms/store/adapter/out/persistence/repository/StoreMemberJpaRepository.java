package com.kb.wms.store.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.store.adapter.out.persistence.entity.StoreMemberJpaEntity;

public interface StoreMemberJpaRepository extends JpaRepository<StoreMemberJpaEntity, Long> {

    boolean existsByStoreIdAndUserId(Long storeId, Long userId);

    @Query("""
            select m from StoreMemberJpaEntity m
            where m.userId = :userId
            order by m.assignedAt desc, m.storeMemberId desc
            """)
    List<StoreMemberJpaEntity> findByUserId(@Param("userId") Long userId);

    @Query("""
            select m from StoreMemberJpaEntity m
            where (:storeId is null or m.storeId = :storeId)
              and (:userId is null or m.userId = :userId)
            order by m.assignedAt desc, m.storeMemberId desc
            """)
    List<StoreMemberJpaEntity> findAllByFilter(@Param("storeId") Long storeId, @Param("userId") Long userId);
}

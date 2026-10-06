package com.kb.wms.store.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.store.adapter.out.persistence.entity.StoreMemberJpaEntity;
import com.kb.wms.store.application.port.in.result.StoreMemberView;

public interface StoreMemberJpaRepository extends JpaRepository<StoreMemberJpaEntity, Long> {

    boolean existsByStoreIdAndUserId(Long storeId, Long userId);

    @Query("""
            select m from StoreMemberJpaEntity m
            where m.userId = :userId
            order by m.assignedAt desc, m.storeMemberId desc
            """)
    List<StoreMemberJpaEntity> findByUserId(@Param("userId") Long userId);

    /** 사용자 테이블을 ID로 조인해 이름·로그인 아이디를 붙이고 keyword로 거른다(읽기 전용 조인). */
    @Query("""
            select new com.kb.wms.store.application.port.in.result.StoreMemberView(
                m.storeMemberId, m.storeId, m.userId, u.name, u.loginId, m.memberRole, m.assignedAt)
            from StoreMemberJpaEntity m
            join UserJpaEntity u on u.userId = m.userId
            where (:storeId is null or m.storeId = :storeId)
              and (:userId is null or m.userId = :userId)
              and (:keyword is null
                   or lower(u.name) like lower(concat('%', :keyword, '%'))
                   or lower(u.loginId) like lower(concat('%', :keyword, '%')))
            order by m.assignedAt desc, m.storeMemberId desc
            """)
    List<StoreMemberView> search(@Param("storeId") Long storeId,
                                 @Param("userId") Long userId,
                                 @Param("keyword") String keyword);
}

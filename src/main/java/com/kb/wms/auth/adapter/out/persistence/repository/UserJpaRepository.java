package com.kb.wms.auth.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.auth.adapter.out.persistence.entity.UserJpaEntity;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.common.security.UserRole;

import jakarta.persistence.LockModeType;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserJpaEntity u where u.userId = :userId")
    Optional<UserJpaEntity> findByIdForUpdate(@Param("userId") Long userId);

    Optional<UserJpaEntity> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndUserIdNot(String email, Long userId);

    boolean existsByRole(UserRole role);

    /** 정렬·페이징은 {@code Pageable}이 정한다. */
    @Query(value = """
            select u from UserJpaEntity u
            where (:role is null or u.role = :role)
              and (:status is null or u.status = :status)
              and (:keyword is null
                   or lower(u.name) like lower(concat('%', :keyword, '%'))
                   or lower(u.loginId) like lower(concat('%', :keyword, '%'))
                   or lower(u.email) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
            select count(u) from UserJpaEntity u
            where (:role is null or u.role = :role)
              and (:status is null or u.status = :status)
              and (:keyword is null
                   or lower(u.name) like lower(concat('%', :keyword, '%'))
                   or lower(u.loginId) like lower(concat('%', :keyword, '%'))
                   or lower(u.email) like lower(concat('%', :keyword, '%')))
            """)
    Page<UserJpaEntity> search(@Param("role") UserRole role,
                               @Param("status") UserStatus status,
                               @Param("keyword") String keyword,
                               Pageable pageable);
}

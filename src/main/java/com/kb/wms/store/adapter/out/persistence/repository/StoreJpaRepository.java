package com.kb.wms.store.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.store.adapter.out.persistence.entity.StoreJpaEntity;
import com.kb.wms.store.domain.enums.StoreStatus;

public interface StoreJpaRepository extends JpaRepository<StoreJpaEntity, Long> {

    boolean existsByStoreCode(String storeCode);

    /** 정렬·페이징은 {@code Pageable}이 정한다. */
    @Query(value = """
            select s from StoreJpaEntity s
            where (:status is null or s.status = :status)
              and (:keyword is null
                   or lower(s.name) like lower(concat('%', :keyword, '%'))
                   or lower(s.storeCode) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
            select count(s) from StoreJpaEntity s
            where (:status is null or s.status = :status)
              and (:keyword is null
                   or lower(s.name) like lower(concat('%', :keyword, '%'))
                   or lower(s.storeCode) like lower(concat('%', :keyword, '%')))
            """)
    Page<StoreJpaEntity> search(@Param("keyword") String keyword, @Param("status") StoreStatus status,
                                Pageable pageable);
}

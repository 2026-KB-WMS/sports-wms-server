package com.kb.wms.store.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.store.adapter.out.persistence.entity.StoreJpaEntity;
import com.kb.wms.store.domain.enums.StoreStatus;

public interface StoreJpaRepository extends JpaRepository<StoreJpaEntity, Long> {

    boolean existsByStoreCode(String storeCode);

    @Query("""
            select s from StoreJpaEntity s
            where (:status is null or s.status = :status)
              and (:keyword is null
                   or lower(s.name) like lower(concat('%', :keyword, '%'))
                   or lower(s.storeCode) like lower(concat('%', :keyword, '%')))
            order by s.createdAt desc, s.storeId desc
            """)
    List<StoreJpaEntity> search(@Param("keyword") String keyword, @Param("status") StoreStatus status);
}

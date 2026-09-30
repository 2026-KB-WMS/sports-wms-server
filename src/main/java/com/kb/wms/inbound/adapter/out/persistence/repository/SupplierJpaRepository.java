package com.kb.wms.inbound.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.inbound.adapter.out.persistence.entity.SupplierJpaEntity;
import com.kb.wms.inbound.domain.enums.SupplierStatus;

public interface SupplierJpaRepository extends JpaRepository<SupplierJpaEntity, Long> {

    boolean existsBySupplierCode(String supplierCode);

    @Query("""
            select s from SupplierJpaEntity s
            where (:status is null or s.status = :status)
              and (:keyword is null
                   or lower(s.name) like lower(concat('%', :keyword, '%'))
                   or lower(s.supplierCode) like lower(concat('%', :keyword, '%'))
                   or lower(s.managerName) like lower(concat('%', :keyword, '%')))
            order by s.name asc, s.supplierId asc
            """)
    List<SupplierJpaEntity> search(@Param("keyword") String keyword, @Param("status") SupplierStatus status);
}

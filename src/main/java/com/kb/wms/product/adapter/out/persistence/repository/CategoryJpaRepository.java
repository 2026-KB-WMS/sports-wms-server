package com.kb.wms.product.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.product.adapter.out.persistence.entity.CategoryJpaEntity;
import com.kb.wms.product.domain.enums.ProductStatus;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {

    boolean existsByCategoryCode(String categoryCode);

    @Query("""
            select c from CategoryJpaEntity c
            where (:parentCategoryId is null or c.parentCategoryId = :parentCategoryId)
              and (:depth is null or c.depth = :depth)
              and (:status is null or c.status = :status)
              and (:keyword is null
                   or lower(c.name) like lower(concat('%', :keyword, '%'))
                   or lower(c.categoryCode) like lower(concat('%', :keyword, '%')))
            order by c.sortOrder, c.categoryId
            """)
    List<CategoryJpaEntity> search(@Param("parentCategoryId") Long parentCategoryId,
                                   @Param("depth") Integer depth,
                                   @Param("keyword") String keyword,
                                   @Param("status") ProductStatus status);
}

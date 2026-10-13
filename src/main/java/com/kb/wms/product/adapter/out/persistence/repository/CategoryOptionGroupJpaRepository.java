package com.kb.wms.product.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.product.adapter.out.persistence.entity.CategoryOptionGroupJpaEntity;

public interface CategoryOptionGroupJpaRepository extends JpaRepository<CategoryOptionGroupJpaEntity, Long> {

    List<CategoryOptionGroupJpaEntity> findByCategoryIdIn(Collection<Long> categoryIds);

    /** 엔티티 삭제는 flush 때 INSERT보다 늦게 실행되어 unique 제약에 걸리므로 벌크 삭제로 바로 실행한다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CategoryOptionGroupJpaEntity c where c.categoryId = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);
}

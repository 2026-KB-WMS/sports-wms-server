package com.kb.wms.common.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.product.adapter.out.persistence.entity.ProductJpaEntity;

/**
 * {@link PageAdapter}가 만든 {@code Pageable}이 도메인 목록 쿼리와 같은 모양(생성자 프로젝션 + 조인 + 별칭 경로 정렬)의
 * JPQL에서 동작하는지 확인하는 테스트 전용 저장소. 도메인 서브이슈가 이 모양을 그대로 쓴다.
 */
public interface PagingProbeRepository extends JpaRepository<ProductJpaEntity, Long> {

    @Query(value = """
            select new com.kb.wms.common.persistence.PagingProbeRow(p.productId, p.name, b.name)
            from ProductJpaEntity p
            join BrandJpaEntity b on b.brandId = p.brandId
            where (:keyword is null or p.name like concat('%', :keyword, '%'))
            """,
            countQuery = """
            select count(p)
            from ProductJpaEntity p
            join BrandJpaEntity b on b.brandId = p.brandId
            where (:keyword is null or p.name like concat('%', :keyword, '%'))
            """)
    Page<PagingProbeRow> search(@Param("keyword") String keyword, Pageable pageable);

    /** 집계(group by) 목록 모양: 집계식으로 정렬할 수 있는지, 건수 쿼리가 따로 맞는지 확인한다. */
    @Query(value = """
            select new com.kb.wms.common.persistence.PagingProbeBrandRow(b.brandId, b.name, count(p), sum(p.productId))
            from BrandJpaEntity b
            join ProductJpaEntity p on p.brandId = b.brandId
            group by b.brandId, b.name
            """,
            countQuery = """
            select count(distinct b.brandId)
            from BrandJpaEntity b
            join ProductJpaEntity p on p.brandId = b.brandId
            """)
    Page<PagingProbeBrandRow> searchBrands(Pageable pageable);
}

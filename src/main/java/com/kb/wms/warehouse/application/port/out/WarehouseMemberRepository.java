package com.kb.wms.warehouse.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMemberView;
import com.kb.wms.warehouse.domain.entity.WarehouseMember;

/**
 * 창고 관리자 배정 영속성 아웃바운드 포트.
 */
public interface WarehouseMemberRepository {

    WarehouseMember save(WarehouseMember member);

    Optional<WarehouseMember> findById(Long warehouseMemberId);

    /**
     * 사용자의 이름·로그인 아이디를 붙여 조회한다. 조건이 null이면 무시하고, keyword는 이름·로그인 아이디
     * 부분 일치(대소문자 무시)다. 정렬·페이지는 {@code page}가 정한다.
     */
    PageResult<WarehouseMemberView> search(Long warehouseId, Long userId, String keyword, PageCondition page);

    List<WarehouseMember> findByUserId(Long userId);

    boolean existsByWarehouseIdAndUserId(Long warehouseId, Long userId);

    void deleteById(Long warehouseMemberId);
}

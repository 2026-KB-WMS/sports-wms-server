package com.kb.wms.store.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.store.domain.entity.StoreMember;

/**
 * 지점 관리자 배정 영속성 아웃바운드 포트.
 */
public interface StoreMemberRepository {

    StoreMember save(StoreMember member);

    Optional<StoreMember> findById(Long storeMemberId);

    /**
     * storeId·userId가 null이면 해당 조건을 무시하고 조회한다. 배정 일시 내림차순.
     */
    List<StoreMember> findAll(Long storeId, Long userId);

    /** 사용자가 배정된 지점 소속 목록. 배정 일시 내림차순. */
    List<StoreMember> findByUserId(Long userId);

    boolean existsByStoreIdAndUserId(Long storeId, Long userId);

    void deleteById(Long storeMemberId);
}

package com.kb.wms.store.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.store.application.port.in.result.StoreMemberView;
import com.kb.wms.store.domain.entity.StoreMember;

/**
 * 지점 관리자 배정 영속성 아웃바운드 포트.
 */
public interface StoreMemberRepository {

    StoreMember save(StoreMember member);

    Optional<StoreMember> findById(Long storeMemberId);

    /**
     * 사용자의 이름·로그인 아이디를 붙여 조회한다. 조건이 null이면 무시하고, keyword는 이름·로그인 아이디
     * 부분 일치(대소문자 무시)다. 배정 일시 내림차순.
     */
    List<StoreMemberView> search(Long storeId, Long userId, String keyword);

    /** 사용자가 배정된 지점 소속 목록. 배정 일시 내림차순. */
    List<StoreMember> findByUserId(Long userId);

    boolean existsByStoreIdAndUserId(Long storeId, Long userId);

    void deleteById(Long storeMemberId);
}

package com.kb.wms.warehouse.application.port.out;

/**
 * 창고 관리자 배정 때 사용자의 이름·역할·활성 여부를 확인하는 아웃바운드 포트. 구현은 인증 도메인의 `port.in`에 연결하는 어댑터가 맡는다.
 */
public interface WarehouseMemberUserPort {

    /** 사용자를 조회한다. 없으면 404 USER_NOT_FOUND. */
    Candidate getCandidate(Long userId);

    /**
     * 배정 후보 사용자 정보.
     *
     * @param role   이 도메인이 구분하는 역할. 창고 관리자가 아니면 OTHER
     * @param active 활성(승인 완료) 상태 여부
     */
    record Candidate(String name, String loginId, MemberRole role, boolean active) {

        public boolean hasAssignableRole() {
            return role == MemberRole.WAREHOUSE_MANAGER;
        }
    }

    /** 인증 도메인의 역할을 이 도메인이 필요한 만큼만 구분한 값. 변환은 어댑터가 한다. */
    enum MemberRole {
        WAREHOUSE_MANAGER,
        OTHER
    }
}

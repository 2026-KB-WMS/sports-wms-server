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
     * @param role   역할 코드 문자열
     * @param active 활성(승인 완료) 상태 여부
     */
    record Candidate(String name, String loginId, String role, boolean active) {

        private static final String ASSIGNABLE_ROLE = "WAREHOUSE_MANAGER";

        public boolean hasAssignableRole() {
            return ASSIGNABLE_ROLE.equals(role);
        }
    }
}

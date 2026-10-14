package com.kb.wms.common.security;

/**
 * 사용자 역할. 인증된 사용자 정보와 보안 필터가 함께 쓰는 공통 개념이라 common이 소유한다.
 * 역할에 따라 호출 가능한 API와 접근 가능한 창고·지점 범위가 달라진다.
 */
public enum UserRole {
    /** 본사 관리자. 마스터 데이터·계정 관리 권한. 가입 API로 만들 수 없다. */
    HQ_ADMIN,
    /** 창고 관리자. 배정된 창고의 입고·재고·출고를 처리한다. */
    WAREHOUSE_MANAGER,
    /** 점주. 배정된 지점의 발주를 처리한다. */
    STORE_OWNER;

    /** 가입 신청(POST /auth/signup)으로 선택할 수 있는 역할인가. */
    public boolean isSelfSignupAllowed() {
        return this != HQ_ADMIN;
    }

    /** 창고·지점 소속이 있어야 업무를 볼 수 있는 역할인가. (승인 시 소속 배정 필수) */
    public boolean requiresAffiliation() {
        return this == WAREHOUSE_MANAGER || this == STORE_OWNER;
    }
}

package com.kb.wms.store.domain.enums;

/**
 * 점주 배정 시 지정하는 지점 내 담당 역할(StoreMember.member_role) 코드.
 * GET /api/v1/stores/management-types 응답 및 POST /stores/assign의 memberRole 검증에 사용한다.
 */
public enum StoreManagementType {

    OWNER("점주"),
    MANAGER("지점 관리자");

    private final String description;

    StoreManagementType(String description) {
        this.description = description;
    }

    public String getCode() {
        return name();
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValidCode(String code) {
        for (StoreManagementType type : values()) {
            if (type.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}

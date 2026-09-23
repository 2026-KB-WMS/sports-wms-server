package com.kb.wms.warehouse.domain.enums;

/**
 * 창고 관리자 배정 시 지정하는 창고 내 담당 역할(WarehouseMember.member_role) 코드.
 * GET /api/v1/warehouses/management-types 응답 및 POST /warehouses/managers의 memberRole 검증에 사용한다.
 * ERD상 허용 값이 확정되지 않아, 현재는 확정된 코드인 MANAGER만 정의한다.
 */
public enum WarehouseManagementType {

    MANAGER("창고 관리자");

    private final String description;

    WarehouseManagementType(String description) {
        this.description = description;
    }

    public String getCode() {
        return name();
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValidCode(String code) {
        for (WarehouseManagementType type : values()) {
            if (type.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}

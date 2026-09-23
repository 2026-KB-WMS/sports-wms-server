package com.kb.wms.warehouse.domain.enums;

/**
 * 창고 구역 유형(WarehouseSection.section_type) 코드.
 * GET /api/v1/warehouses/section-types 응답 및 구역 등록·수정 API의 sectionType 검증에 사용한다.
 * DEFECT(불량 구역)는 확정 코드이고, ZONE/RACK은 ERD 예시를 그대로 반영한 값이다.
 */
public enum WarehouseSectionType {

    ZONE("구역"),
    RACK("랙"),
    DEFECT("불량 구역");

    private final String description;

    WarehouseSectionType(String description) {
        this.description = description;
    }

    public String getCode() {
        return name();
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValidCode(String code) {
        for (WarehouseSectionType type : values()) {
            if (type.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}

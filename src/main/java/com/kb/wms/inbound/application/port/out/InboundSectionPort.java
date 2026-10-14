package com.kb.wms.inbound.application.port.out;

/**
 * 창고 도메인의 구역 정보를 입고 도메인에서 확인하기 위한 아웃바운드 포트.
 */
public interface InboundSectionPort {

    /** 구역이 없으면 창고 도메인의 SECTION_NOT_FOUND(404)를 던진다. */
    SectionInfo getSection(Long sectionId);

    /**
     * 검수에서 구역을 검증하는 데 필요한 정보만 담는다.
     *
     * @param kind 입고가 구분하는 구역 종류. 불량 구역이 아니면 OTHER
     */
    record SectionInfo(Long sectionId, Long warehouseId, SectionKind kind, boolean active) {

        public boolean isDefect() {
            return kind == SectionKind.DEFECT;
        }
    }

    /** 창고 도메인의 구역 유형을 입고가 필요한 만큼만 구분한 값. 변환은 어댑터가 한다. */
    enum SectionKind {
        DEFECT,
        OTHER
    }
}

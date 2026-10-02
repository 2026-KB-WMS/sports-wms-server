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
     * @param sectionType 구역 유형 코드(불량 구역은 DEFECT)
     */
    record SectionInfo(Long sectionId, Long warehouseId, String sectionType, boolean active) {

        private static final String DEFECT_TYPE = "DEFECT";

        public boolean isDefect() {
            return DEFECT_TYPE.equals(sectionType);
        }
    }
}

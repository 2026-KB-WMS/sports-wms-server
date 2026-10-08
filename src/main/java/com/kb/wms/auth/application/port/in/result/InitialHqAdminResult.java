package com.kb.wms.auth.application.port.in.result;

/**
 * 최초 본사 관리자 생성 시도의 결과.
 */
public enum InitialHqAdminResult {
    /** 본사 관리자가 없어서 설정값으로 새로 만들었다. */
    CREATED,
    /** 본사 관리자가 이미 있어서 아무것도 하지 않았다. */
    ALREADY_EXISTS,
    /** 본사 관리자가 없지만 생성 설정이 비어 있어 만들지 못했다. */
    NOT_CONFIGURED
}

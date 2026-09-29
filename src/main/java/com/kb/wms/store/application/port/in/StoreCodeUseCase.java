package com.kb.wms.store.application.port.in;

import java.util.List;

import com.kb.wms.store.application.port.in.result.CodeItem;

/**
 * 지점 도메인 고정 코드 목록 조회 유스케이스.
 * GET /api/v1/stores/management-types
 */
public interface StoreCodeUseCase {

    List<CodeItem> getManagementTypes();
}

package com.kb.wms.store.application.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.store.application.port.in.StoreCodeUseCase;
import com.kb.wms.store.application.port.in.result.CodeItem;
import com.kb.wms.store.domain.enums.StoreManagementType;

/**
 * 지점 도메인 고정 코드 목록 조회. DB 조회 없이 확정된 enum 값을 그대로 반환한다.
 */
@Service
@Transactional(readOnly = true)
public class StoreCodeService implements StoreCodeUseCase {

    @Override
    public List<CodeItem> getManagementTypes() {
        return Arrays.stream(StoreManagementType.values())
                .map(type -> new CodeItem(type.getCode(), type.getDescription()))
                .toList();
    }
}

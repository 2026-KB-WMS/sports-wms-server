package com.kb.wms.warehouse.application.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.warehouse.application.port.in.WarehouseCodeUseCase;
import com.kb.wms.warehouse.application.port.in.result.CodeItem;
import com.kb.wms.warehouse.domain.enums.WarehouseManagementType;
import com.kb.wms.warehouse.domain.enums.WarehouseSectionType;

/**
 * 창고 도메인 고정 코드 목록 조회. DB 조회 없이 확정된 enum 값을 그대로 반환한다.
 */
@Service
@Transactional(readOnly = true)
public class WarehouseCodeService implements WarehouseCodeUseCase {

    @Override
    public List<CodeItem> getManagementTypes() {
        return Arrays.stream(WarehouseManagementType.values())
                .map(type -> new CodeItem(type.getCode(), type.getDescription()))
                .toList();
    }

    @Override
    public List<CodeItem> getSectionTypes() {
        return Arrays.stream(WarehouseSectionType.values())
                .map(type -> new CodeItem(type.getCode(), type.getDescription()))
                .toList();
    }
}

package com.kb.wms.store.application.port.in.command;

/**
 * POST /api/v1/stores/assign 요청.
 *
 * @param memberRole 지점 내 담당 역할 코드 (StoreManagementType: OWNER, MANAGER)
 */
public record StoreMemberAssignCommand(
        Long storeId,
        Long userId,
        String memberRole
) {
}

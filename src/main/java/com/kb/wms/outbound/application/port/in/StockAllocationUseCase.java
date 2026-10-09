package com.kb.wms.outbound.application.port.in;

import java.util.List;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;
import com.kb.wms.outbound.application.port.in.command.StockAllocationReleaseCommand;
import com.kb.wms.outbound.application.port.in.query.StockAllocationSearchCondition;
import com.kb.wms.outbound.application.port.in.result.StockAllocateResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationDetail;
import com.kb.wms.outbound.application.port.in.result.StockAllocationReleaseResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationSummary;

/**
 * 재고 할당 유스케이스: 발주 FEFO 할당, 할당 해제, 조회.
 */
public interface StockAllocationUseCase {

    /**
     * 배정된(ASSIGNED) 발주의 잔여 수량을 FEFO 순서로 재고 행에 예약한다. 전체 항목을 한 번에 처리하며(all-or-nothing)
     * 한 항목이라도 가용 재고가 부족하면 아무것도 할당하지 않는다.
     */
    StockAllocateResult allocate(StockAllocateCommand command, AuthenticatedUser actor);

    /** ALLOCATED 할당을 해제한다. 취소되지 않은 출고에 연결된 할당은 해제할 수 없다. */
    StockAllocationReleaseResult release(StockAllocationReleaseCommand command, AuthenticatedUser actor);

    List<StockAllocationSummary> searchAllocations(StockAllocationSearchCondition condition, AuthenticatedUser actor);

    StockAllocationDetail getAllocation(Long allocationId, AuthenticatedUser actor);
}